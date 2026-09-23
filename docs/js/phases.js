/* 前端 · 选课阶段（selection_period）解析
 *
 * 这一层解决一个具体问题：**倒计时到底该盯哪个时刻**。
 *
 * 改造前：倒计时盯的是后端启动时间 / 页面加载时间 + 90 秒，纯演示假数据，
 *         跟数据库里的 selection_period 表没有任何关系，教务调了时间前端也不会变。
 * 改造后：后端把阶段表以 phases[] 下发（Java 版读 selection_period，
 *         静态版读 data.js 的 periodState），本模块负责从阶段里选出
 *         「下一个值得倒计时的时刻」并给出阶段语境。
 *
 * 选取规则（与后端 selectionOpenAt() 同一套口径）：
 *   1. 有进行中的阶段 → 倒计时目标就是它的开始时间（即已开放，倒计时归零）；
 *   2. 否则取最近一个未开始阶段的开始时间；
 *   3. 阶段表为空 → 回退到后端给的 openAt；
 *   4. openAt 也没有 → 返回 null（前端不显示倒计时，而不是显示假的 90 秒）。
 *
 * 时间字符串兼容：后端 Java 版用 jackson 输出 "yyyy-MM-dd HH:mm:ss"，
 * 静态版也用同样形状。Safari 对 "2026-09-25 09:00:00" 解析不稳，
 * 因此这里统一把空格换成 T 再交给 Date 解析。
 */

/** 把后端的时间字符串解析成毫秒时间戳；无法解析返回 null */
export function parseTime(v) {
  if (v == null || v === "") return null;
  if (typeof v === "number") return Number.isFinite(v) ? v : null;
  const s = String(v).trim().replace(" ", "T");
  const t = Date.parse(s);
  return Number.isNaN(t) ? null : t;
}

/** 阶段状态（以后端下发为准，缺字段时按时间兜底推算） */
export function statusOf(phase, now = Date.now()) {
  if (!phase) return "UNKNOWN";
  if (phase.status) return phase.status;
  const start = parseTime(phase.start);
  const end = parseTime(phase.end);
  if (start == null || end == null) return "UNKNOWN";
  if (now > end) return "FINISHED";
  if (now >= start) return "ONGOING";
  return "NOT_STARTED";
}

/** 进行中的阶段（无则 null） */
export function currentPhase(phases) {
  const list = Array.isArray(phases) ? phases : [];
  return list.find((p) => p && p.current) || list.find((p) => statusOf(p) === "ONGOING") || null;
}

/** 下一个尚未开始的阶段（按开始时间升序取第一个，无则 null） */
export function nextPhase(phases) {
  const list = (Array.isArray(phases) ? phases : [])
    .filter((p) => p && statusOf(p) === "NOT_STARTED")
    .map((p) => ({ p, t: parseTime(p.start) }))
    .filter((x) => x.t != null)
    .sort((a, b) => a.t - b.t);
  return list.length ? list[0].p : null;
}

/**
 * 从后端返回的 status 载荷里解析出倒计时语境。
 *
 * @param {{phases?: Array, openAt?: number|string, selectionOpen?: boolean}} statusData
 * @returns {{target: number|null, phase: object|null, phaseName: string,
 *            status: string, windowText: string, closed: boolean, source: string}}
 *   target     — 倒计时目标毫秒时间戳，null 表示不该显示倒计时
 *   phase      — 参与倒计时的阶段对象
 *   phaseName  — 阶段名（「正选」「补退选」…），无阶段时为「选课」
 *   status     — 该阶段的 ONGOING / NOT_STARTED / …
 *   windowText — "09-25 09:00 ~ 09-28 23:59" 形状的起止区间
 *   closed     — 教务是否手动关闭了选课通道
 *   source     — 取值来源，便于排查：phase-current / phase-next / openAt / none
 */
export function resolveCountdown(statusData = {}) {
  const phases = Array.isArray(statusData.phases) ? statusData.phases : [];
  const closed = statusData.selectionOpen === false;
  const cur = currentPhase(phases);

  if (cur) {
    return {
      target: parseTime(cur.start),
      phase: cur,
      phaseName: cur.name || "选课",
      status: "ONGOING",
      windowText: windowTextOf(cur),
      closed,
      source: "phase-current",
      /* 视图层需要完整阶段列表来渲染「选课节点」卡片与时间线 */
      phases,
    };
  }

  const nxt = nextPhase(phases);
  if (nxt) {
    return {
      target: parseTime(nxt.start),
      phase: nxt,
      phaseName: nxt.name || "选课",
      status: "NOT_STARTED",
      windowText: windowTextOf(nxt),
      closed,
      source: "phase-next",
      phases,
    };
  }

  const openAt = parseTime(statusData.openAt);
  if (openAt != null) {
    return {
      target: openAt,
      phase: null,
      phaseName: "选课",
      status: "NOT_STARTED",
      windowText: "",
      closed,
      source: "openAt",
      phases,
    };
  }

  return {
    target: null, phase: null, phaseName: "选课",
    status: "UNKNOWN", windowText: "", closed,
    source: "none",
    phases,
  };
}

/** "2026-09-25 09:00:00" -> "09-25 09:00 ~ 09-28 23:59" */
export function windowTextOf(phase) {
  if (!phase) return "";
  const s = String(phase.start || "").slice(5, 16);
  const e = String(phase.end || "").slice(5, 16);
  if (!s && !e) return "";
  if (!e) return s;
  return `${s} ~ ${e}`;
}

/** 未开始时给出「还有多久开」的粗略中文描述，用于提示语 */
export function roughEta(target, now = Date.now()) {
  if (target == null) return "";
  const ms = target - now;
  if (ms <= 0) return "已开放";
  const d = Math.floor(ms / 86400e3);
  const h = Math.floor((ms % 86400e3) / 3600e3);
  const m = Math.floor((ms % 3600e3) / 60e3);
  if (d > 0) return `${d} 天后`;
  if (h > 0) return `${h} 小时后`;
  if (m > 0) return `${m} 分钟后`;
  return "不到 1 分钟";
}
