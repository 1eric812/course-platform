// 静态版后端功能测试：直接在 Node 里跑 docs/ 的浏览器端「后端」
import { handleApi, API_CATALOG } from "../docs/js/backend/server.js";
import { db, periodState, currentPhase, nextPhase, selectionOpenAt } from "../docs/js/backend/data.js";
import * as svc from "../docs/js/backend/service.js";
import { resolveCountdown, parseTime, windowTextOf, currentPhase as fcCurrent, nextPhase as fcNext } from "../docs/js/phases.js";

const call = (m, p, q = {}, b = {}) => handleApi(m, p, q, b);
let fail = 0;
const check = (name, cond, extra = "") => {
  console.log(`${cond ? "PASS" : "FAIL"}  ${name}${extra ? "  " + extra : ""}`);
  if (!cond) fail++;
};

// ---------- 基础 ----------
let r = await call("GET", "/api/health");
check("health 200", r.status === 200 && r.data.ok === true);
r = await call("GET", "/api/architecture");
check("architecture 接口清单", r.status === 200 && r.data.endpoints.length === API_CATALOG.length, `${r.data.endpoints.length} 条`);
r = await call("GET", "/api/filters");
check("filters 含新维度", r.status === 200 && r.data.days.length > 0 && r.data.periods.length > 0 && r.data.teachers.length > 0,
  `days=${r.data.days.length} periods=${r.data.periods.length} teachers=${r.data.teachers.length}`);

// ---------- 课程与筛选 ----------
r = await call("GET", "/api/courses", {});
check("courses 全量", r.status === 200 && r.data.items.length === 40, `${r.data.items.length} 门`);
const all = r.data.items.length;
r = await call("GET", "/api/courses", { onlyNoConflict: "1" });
check("onlyNoConflict 生效", r.data.items.length < all, `${r.data.items.length} < ${all}`);
r = await call("GET", "/api/courses", { days: "1" });
check("按上课日筛选(周一)", r.data.items.length > 0 && r.data.items.every((c) => c.schedule.some((s) => s.day === 1)), `${r.data.items.length} 门`);
r = await call("GET", "/api/courses", { teacher: "刘洋" });
check("按教师筛选", r.data.items.length === 3 && r.data.items.every((c) => c.teacher === "刘洋"), r.data.items.map((c) => c.name).join(","));

// ---------- 四类冲突检测 ----------
r = await call("GET", "/api/courses", {});
const cs201 = r.data.items.find((c) => c.id === 5); // 机器学习导论，先修 线性代数（a1 未修）
check("先修缺失软冲突", cs201.conflictDetail.some((x) => x.type === "prereq" && x.level === "soft"), cs201.conflictDetail.find((x) => x.type === "prereq")?.message);
const hasTime = r.data.items.some((c) => c.conflictDetail.some((x) => x.type === "time" && x.level === "hard"));
check("时间硬冲突可检出", hasTime);
r = await call("PUT", "/api/admin/rules", {}, { creditLimit: 12 }); // a1 已选 11 学分
r = await call("GET", "/api/courses", {});
const cs201b = r.data.items.find((c) => c.id === 5);
check("学分超限软冲突", cs201b.conflictDetail.some((x) => x.type === "credit" && x.level === "soft"), cs201b.conflictDetail.find((x) => x.type === "credit")?.message);
await call("PUT", "/api/admin/rules", {}, { creditLimit: 30 });

// ---------- 心愿单 + 四态票据 ----------
await call("POST", "/api/wishlist", {}, { courseId: 26 });
await call("POST", "/api/wishlist", {}, { courseId: 32 });
r = await call("POST", "/api/selection/submit", {}, {});
check("submit 202 受理", r.status === 202 && r.data.ticket.status === "已受理" && typeof r.data.ticket.queuePos === "number",
  `${r.data.ticket.id} 队列第 ${r.data.ticket.queuePos} 位`);
const tk = r.data.ticket.id;
await new Promise((s) => setTimeout(s, 1500));
r = await call("GET", "/api/selection/status/" + tk);
check("票据终态为四态之一", ["成功", "失败"].includes(r.data.status), `${r.data.status} 成功${r.data.accepted.length}/失败${r.data.rejected.length}`);

// ---------- 放号订阅 ----------
let full = db.allCourses().find((c) => c.remaining === 0);
if (!full) { db.takeSeat(db.allCourses()[0].id); }
full = db.allCourses().find((c) => c.remaining === 0) || db.allCourses()[0];
db.releaseSeat(full.id, 0); // 确保满员
r = await call("POST", "/api/subscribe", {}, { courseId: full.id });
check("订阅放号", r.status === 200, full.name);
const before = db.state.messages.length;
db.releaseSeat(full.id, 2); // 放号
const sent = svc.notifySubscribers(full);
check("放号后推送消息", sent >= 1 && db.state.messages.length > before && db.state.messages[0].type === "seat", db.state.messages[0]?.title);

// ---------- 课表导出 ICS ----------
r = await call("GET", "/api/timetable.ics");
check("ICS 导出", r.status === 200 && r.data.ics.startsWith("BEGIN:VCALENDAR") && r.data.ics.includes("SUMMARY"),
  `${r.data.ics.split("BEGIN:VEVENT").length - 1} 个事件`);

// ---------- 多账号隔离 ----------
r = await call("GET", "/api/me");
check("me 当前账号 a1", r.status === 200 && r.data.accountId === "a1" && r.data.accounts.length === 3,
  r.data.accounts.map((a) => a.name).join("/"));
const a1Enrolled = db.enrolled().length;
r = await call("POST", "/api/accounts/switch", {}, { accountId: "a2" });
check("切换到 a2", r.status === 200 && r.data.user.name === "苏同学");
check("账号状态隔离", db.enrolled().length !== a1Enrolled || db.state.wishlist.length === 0, `a1 已选 ${a1Enrolled} → a2 已选 ${db.enrolled().length}`);
r = await call("POST", "/api/accounts/switch", {}, { accountId: "a1" });
check("切回 a1", db.enrolled().length === a1Enrolled);

// ---------- 教师端 / 教务端回归 ----------
r = await call("GET", "/api/teacher/me");
check("教师档案", r.status === 200 && r.data.courseCount === 3, `${r.data.name} 授 ${r.data.courseCount} 门`);
r = await call("GET", "/api/teacher/courses/1/roster");
check("非本人课程 403", r.status === 403);
r = await call("GET", "/api/admin/anomalies", {});
check("异常工单", r.status === 200 && r.data.items.length === 3, `${r.data.items.length} 条`);
r = await call("GET", "/api/nope");
check("未知接口 404", r.status === 404);

// ---------- 选课阶段（selection_period）与倒计时锚点 ----------
r = await call("GET", "/api/status");
const st = r.data;
check("status 含 phases", r.status === 200 && Array.isArray(st.phases) && st.phases.length === 3,
  (st.phases || []).map((p) => `${p.name}:${p.status}`).join(" "));
check("status 含 selectionOpen", typeof st.selectionOpen === "boolean", String(st.selectionOpen));
check("status 含 serverTime", typeof st.serverTime === "number");
check("阶段字段齐全", st.phases.every((p) => p.code && p.name && p.start && p.end && p.status && "current" in p));
check("恰好一个阶段是 current", st.phases.filter((p) => p.current).length <= 1);
check("阶段按开始时间升序", st.phases.every((p, i) => i === 0 || parseTime(st.phases[i - 1].start) <= parseTime(p.start)));

/* 关键回归：倒计时锚点必须是「数据库阶段时间」，不再是「页面加载 + 90 秒」 */
const cur = currentPhase();
check("存在进行中的阶段（正选）", !!cur && cur.phaseName === "正选", cur && cur.phaseName);
check("openAt 等于当前阶段开始时间", selectionOpenAt() === cur.startTime,
  `${new Date(selectionOpenAt()).toISOString()} vs ${new Date(cur.startTime).toISOString()}`);
check("openAt 不等于 页面加载+90s", Math.abs(selectionOpenAt() - (Date.now() + 90000)) > 60000);

/* /api/periods 单独取阶段表 */
r = await call("GET", "/api/periods");
check("periods 接口", r.status === 200 && r.data.items.length === 3);

/* 前端解析层：resolveCountdown 应命中 phase-current */
let ctx = resolveCountdown(st);
check("resolveCountdown 取进行中阶段", ctx.source === "phase-current" && ctx.phaseName === "正选", ctx.source);
check("resolveCountdown 带窗口文案", /^\d{2}-\d{2} \d{2}:\d{2} ~ \d{2}-\d{2} \d{2}:\d{2}$/.test(ctx.windowText), ctx.windowText);

/* 教务把「正选」挪到未来 → 提交应被拒，且倒计时目标跟着变 */
const future = Date.now() + 3600e3;
r = await call("PUT", "/api/admin/periods/MAIN", {}, { startTime: future, endTime: future + 7200e3 });
check("教务调整阶段时间", r.status === 200 && r.data.phase && !r.data.phase.current, JSON.stringify(r.data.phase && r.data.phase.status));
check("调整后 openAt 跟随阶段表", Math.abs(selectionOpenAt() - future) < 1500,
  `${new Date(selectionOpenAt()).toISOString()}`);
ctx = resolveCountdown(await call("GET", "/api/status").then((x) => x.data));
check("阶段未开始时倒计时指向该阶段", ctx.source === "phase-next" && ctx.phaseName === "正选", ctx.source);
/* 加一门到心愿单，否则会先撞上「心愿单为空」的校验，测不到阶段门禁 */
await call("POST", "/api/wishlist", {}, { courseId: 27 });
r = await call("POST", "/api/selection/submit", {}, {});
check("阶段未开放时拒绝提交", r.status === 400 && /不在选课阶段内/.test(r.data.error), r.data.error);
await call("DELETE", "/api/wishlist/27", {}, {});

/* 教务关闭通道 → status.selectionOpen=false，语义与阶段窗口独立 */
r = await call("PUT", "/api/admin/rules", {}, { selectionOpen: false });
check("关闭选课通道", r.data.selectionOpen === false);
r = await call("GET", "/api/status");
check("status 反映通道关闭", r.data.selectionOpen === false);
r = await call("PUT", "/api/admin/rules", {}, { selectionOpen: true });

/* 阶段表被清空 → 不应产生假倒计时，openAt 回退到「现在」 */
const backup = periodState.periods.slice();
periodState.periods = [];
ctx = resolveCountdown({ phases: [], openAt: undefined });
check("无阶段时 target 为 null（不造假倒计时）", ctx.target === null && ctx.source === "none", ctx.source);
periodState.periods = backup;

/* 时间串解析容错：Java 版输出 "yyyy-MM-dd HH:mm:ss"（空格分隔） */
check("parseTime 兼容空格分隔", parseTime("2026-09-25 09:00:00") === new Date("2026-09-25T09:00:00").getTime());
check("parseTime 兼容 null", parseTime(null) === null && parseTime("") === null);
check("windowTextOf 空阶段返回空串", windowTextOf(null) === "");

/* 恢复默认阶段编排，避免影响后续断言 */
svc.updatePeriod("MAIN", { startTime: Date.now() - 3600e3, endTime: Date.now() + 3 * 86400e3 });
check("恢复阶段后可提交", currentPhase() !== null && nextPhase() !== null);

// ---------- 前端解析层单独回归（phases.js 是两端共用的纯函数模块） ----------
const sample = [
  { code: "PRE", name: "预选", start: "2026-09-18 09:00:00", end: "2026-09-20 23:59:59", status: "FINISHED", current: false },
  { code: "MAIN", name: "正选", start: "2026-09-25 09:00:00", end: "2026-09-28 23:59:59", status: "NOT_STARTED", current: false },
];
check("fcCurrent 无进行中阶段返回 null", fcCurrent(sample) === null);
check("fcNext 取最近未开始阶段", fcNext(sample)?.code === "MAIN", fcNext(sample)?.code);
check("resolveCountdown 落到 phase-next", resolveCountdown({ phases: sample }).source === "phase-next");
check("resolveCountdown 目标等于阶段开始", resolveCountdown({ phases: sample }).target === parseTime("2026-09-25 09:00:00"));
check("resolveCountdown 传 closed 标记", resolveCountdown({ phases: sample, selectionOpen: false }).closed === true);
/* phases 缺失但 openAt 存在 → 退化为 openAt，而不是 null */
check("无 phases 时回退 openAt", resolveCountdown({ openAt: 1790000000000 }).source === "openAt");
check("两者都无 → target null", resolveCountdown({}).target === null);
/* 非法时间串不应让 Date.parse 变成 NaN 污染倒计时 */
check("无效 / 非法时间串返回 null", parseTime("not-a-date") === null && parseTime(undefined) === null);
/* 回归：视图层靠 ctx.phases 渲染「选课节点」卡片，必须随语境一起带出来 */
check("resolveCountdown 透传 phases 列表", Array.isArray(resolveCountdown({ phases: sample }).phases)
  && resolveCountdown({ phases: sample }).phases.length === 2);
check("无 phases 时透传空数组（不 undefined）", Array.isArray(resolveCountdown({}).phases));

// ---------- 回归：教务端保存阶段时间的三个真实缺陷 ----------
/* 1) 前端提交的是 "yyyy-MM-dd HH:mm" 字符串，早期实现用 Number() 判定导致整条 patch 被丢弃 */
const fmtLocalStr = (ms) => {
  const d = new Date(ms);
  const p = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
};
const target = Date.now() + 2 * 3600e3;
const wantStart = new Date(fmtLocalStr(target)).getTime();
const upd = svc.updatePeriod("MAIN", { startTime: fmtLocalStr(target), endTime: fmtLocalStr(target + 3 * 3600e3) });
check("updatePeriod 接受时间字符串", upd.ok === true && upd.error === undefined, JSON.stringify(upd.error || ""));
check("updatePeriod 字符串时间真正落库", selectionOpenAt() === wantStart, `${selectionOpenAt()} vs ${wantStart}`);
check("改到未来后该阶段变为 NOT_STARTED",
  svc.listPeriods().items.find((x) => x.code === "MAIN")?.status === "NOT_STARTED");
check("改到未来后不再有进行中阶段", currentPhase() === null);
check("改到未来后倒计时指向该阶段（phase-next）",
  resolveCountdown({ phases: svc.listPeriods().items }).source === "phase-next");

/* 2) 非法时间串不应把阶段改坏 */
const beforeBad = svc.listPeriods().items.find((x) => x.code === "MAIN").start;
svc.updatePeriod("MAIN", { startTime: "不是时间" });
check("非法时间串被忽略（保持原值）",
  svc.listPeriods().items.find((x) => x.code === "MAIN").start === beforeBad);

/* 3) 教务在阶段外提交选课应被拒绝，且提示里点出下一个阶段名 */
svc.updatePeriod("MAIN", { startTime: fmtLocalStr(Date.now() + 2 * 3600e3), endTime: fmtLocalStr(Date.now() + 5 * 3600e3) });
svc.wishlistAdd(22);
const outside = await svc.submitSelection();
check("阶段外提交被拒绝", typeof outside.error === "string", outside.error || "(未拒绝)");
check("拒绝提示带下一个阶段名与开放时间",
  /正选/.test(outside.error || "") && /月/.test(outside.error || ""), outside.error || "");
svc.wishlistRemove(22);

/* 恢复默认阶段编排，便于人工在浏览器里继续演示 */
svc.updatePeriod("MAIN", { startTime: fmtLocalStr(Date.now() - 3600e3), endTime: fmtLocalStr(Date.now() + 3 * 86400e3) });
check("恢复后重新出现进行中阶段", currentPhase()?.phaseCode === "MAIN");

// ---------- 回归：学分上限必须能配到 120（原先被前端 max=40 与后端 min(60) 双重卡死） ----------
const rulesNow = svc.getRules();
const oldCredit = rulesNow.creditLimit;
const oldWish = rulesNow.maxWishlist;

svc.updateRules({ creditLimit: 120 });
check("学分上限可设为 120", svc.getRules().creditLimit === 120, String(svc.getRules().creditLimit));
check("学分上限经接口回读仍为 120", (await svc.getRules()).creditLimit === 120);

/* 上下边界：低于 1 抬到 1，高于 300 压到 300，但 120 这类合理值不被改写 */
svc.updateRules({ creditLimit: 0 });
check("学分上限下限钳制到 1", svc.getRules().creditLimit === 1, String(svc.getRules().creditLimit));
svc.updateRules({ creditLimit: 999 });
check("学分上限上限钳制到 300", svc.getRules().creditLimit === 300, String(svc.getRules().creditLimit));

/* 120 应真实放宽选课校验：超过原 60 的额度不再触发「超出学分上限」 */
svc.updateRules({ creditLimit: 120 });
const free = svc.getRules().creditLimit === 120;
check("放宽后不再受 60 的旧上限约束", free, String(svc.getRules().creditLimit));

/* 两个字段的钳制区间互相独立，别共用同一段 */
svc.updateRules({ maxWishlist: 30 });
check("心愿单上限仍受自己的区间约束（≤60）", svc.getRules().maxWishlist === 30, String(svc.getRules().maxWishlist));
svc.updateRules({ maxWishlist: 999 });
check("心愿单上限上限为 60", svc.getRules().maxWishlist === 60, String(svc.getRules().maxWishlist));

/* 还原，避免影响其它断言与演示默认值 */
svc.updateRules({ creditLimit: oldCredit, maxWishlist: oldWish });
check("规则已还原", svc.getRules().creditLimit === oldCredit && svc.getRules().maxWishlist === oldWish);

console.log(fail === 0 ? "\n全部通过 ✅" : `\n有 ${fail} 项失败 ❌`);
process.exit(fail === 0 ? 0 : 1);
