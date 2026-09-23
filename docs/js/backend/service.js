/* 后端 · 业务服务层：筛选、冲突检测、心愿单、选课受理 */

import { db, CATEGORIES, currentTeacher, genRoster, adminState, resetDemo, phaseViews, updatePeriod as applyPeriod, currentPhase, nextPhase, persistRules } from "./data.js";
import { config } from "./config.js";

// 静态版：浏览器没有 process，用模块加载时刻代替进程启动时刻
const BOOT_AT = Date.now();
const uptimeSec = () => Math.floor((Date.now() - BOOT_AT) / 1000);

const DAY_NAMES = ["一", "二", "三", "四", "五", "六", "日"];
const CATEGORY_ORDER = ["必修", "选修", "通识", "体育"];

/** 阶段时间的中文短格式，用于「将于 X 开放」这类提示语 */
function fmtPhaseTime(ts) {
  const d = new Date(ts);
  const p = (n) => String(n).padStart(2, "0");
  return `${d.getMonth() + 1} 月 ${d.getDate()} 日 ${p(d.getHours())}:${p(d.getMinutes())}`;
}

/* ---------------- 冲突检测 ---------------- */

function overlap(a, b) {
  return a.day === b.day && a.start <= b.end && b.start <= a.end;
}

function timeConflicts(a, b) {
  const hits = [];
  for (const sa of a.schedule)
    for (const sb of b.schedule)
      if (overlap(sa, sb)) hits.push({ day: sa.day, period: `${Math.max(sa.start, sb.start)}-${Math.min(sa.end, sb.end)}` });
  return hits;
}

function commuteConflict(a, b) {
  if (a.campus === b.campus) return null;
  for (const sa of a.schedule)
    for (const sb of b.schedule) {
      if (sa.day !== sb.day) continue;
      if (Math.abs(sa.end - sb.start) <= 1 || Math.abs(sb.end - sa.start) <= 1)
        return { day: sa.day, from: a.campus, to: b.campus };
    }
  return null;
}

/** 检测一门课与课程集合的冲突。level: hard=阻断, soft=警告（受教务规则影响）。
 *  覆盖 I-02 四类冲突：时间重叠(硬)、跨校区通勤、先修缺失、学分上限超限。 */
export function detectConflicts(course, others) {
  const rules = adminState.rules;
  const result = [];
  for (const o of others) {
    if (!o || o.id === course.id) continue;
    const tc = timeConflicts(course, o);
    if (tc.length)
      result.push({ type: "time", level: rules.blockOnConflict ? "hard" : "soft", courseId: o.id, courseName: o.name,
        message: `与「${o.name}」时间冲突（周${DAY_NAMES[tc[0].day - 1]} 第 ${tc[0].period} 节）` });
    const cc = commuteConflict(course, o);
    if (cc && !result.some((r) => r.courseId === o.id && r.type === "commute"))
      result.push({ type: "commute", level: rules.allowCrossCampus ? "soft" : "hard", courseId: o.id, courseName: o.name,
        message: `与「${o.name}」跨校区连堂（${cc.from} → ${cc.to}），通勤时间紧张` });
  }
  // 先修缺失（软性）：本人尚未修读该课程要求的先修课
  if (course.prereq && !db.enrolled().some((e) => e.name === course.prereq))
    result.push({ type: "prereq", level: "soft", courseId: course.id, courseName: course.name,
      message: `先修要求「${course.prereq}」未修读，建议先完成先修课` });
  // 学分上限超限（软性）：加上本课后将超出上限
  const after = totalCredits([course.id]);
  if (after > rules.creditLimit)
    result.push({ type: "credit", level: "soft", courseId: course.id, courseName: course.name,
      message: `选上后总学分将达 ${after}，超出上限 ${rules.creditLimit} 学分` });
  return result;
}

export function totalCredits(extraIds = []) {
  const ids = new Set([...db.state.enrolledIds, ...extraIds]);
  let sum = 0;
  ids.forEach((id) => { const c = db.courseById(id); if (c) sum += c.credits; });
  return sum;
}

/* ---------------- 课程筛选 ---------------- */

export function listCourses(q = {}) {
  const enrolledIds = new Set(db.state.enrolledIds);
  const wishIds = new Set(db.state.wishlist.map((w) => w.courseId));
  const enrolledCourses = db.enrolled();

  let list = db.allCourses().map((c) => {
    const conflicts = enrolledIds.has(c.id) ? [] : detectConflicts(c, enrolledCourses);
    return {
      ...c, remaining: c.remaining,
      selected: enrolledIds.has(c.id),
      inWishlist: wishIds.has(c.id),
      subscribed: db.state.subscriptions.includes(c.id),
      conflict: conflicts.some((x) => x.level === "hard"),
      conflictDetail: conflicts,
    };
  });

  if (q.keyword) {
    const k = String(q.keyword).trim().toLowerCase();
    list = list.filter((c) =>
      c.name.toLowerCase().includes(k) || c.code.toLowerCase().includes(k) || c.teacher.toLowerCase().includes(k));
  }
  const inSet = (v, s) => !s || String(s).split(",").filter(Boolean).includes(String(v));
  if (q.category) list = list.filter((c) => inSet(c.category, q.category));
  if (q.campus) list = list.filter((c) => inSet(c.campus, q.campus));
  if (q.assessment) list = list.filter((c) => inSet(c.assessment, q.assessment));
  if (q.credits) list = list.filter((c) => inSet(c.credits, q.credits));
  // I-01 新增维度：上课日 / 起始节次 / 授课教师
  if (q.days) {
    const sel = String(q.days).split(",").filter(Boolean).map(Number);
    list = list.filter((c) => c.schedule.some((s) => sel.includes(s.day)));
  }
  if (q.periods) {
    const sel = String(q.periods).split(",").filter(Boolean).map(Number);
    list = list.filter((c) => c.schedule.some((s) => sel.includes(s.start)));
  }
  if (q.teacher) {
    const t = String(q.teacher).trim().toLowerCase();
    if (t) list = list.filter((c) => c.teacher.toLowerCase().includes(t));
  }
  if (q.onlyNoConflict === "1") list = list.filter((c) => !c.conflict);
  if (q.onlyAvailable === "1") list = list.filter((c) => c.remaining > 0);
  if (q.excludeEnrolled === "1") list = list.filter((c) => !c.selected);

  const sorters = {
    remaining: (a, b) => b.remaining - a.remaining,
    rating: (a, b) => b.rating - a.rating,
    credits: (a, b) => b.credits - a.credits,
    default: (a, b) => a.id - b.id,
  };
  list.sort(sorters[q.sort] || sorters.default);

  return { total: list.length, credits: totalCredits(), creditLimit: adminState.rules.creditLimit, items: list };
}

export function filterOptions() {
  const cs = db.allCourses();
  const uniq = (key) => [...new Set(cs.map((c) => c[key]))];
  const days = [...new Set(cs.flatMap((c) => c.schedule.map((s) => s.day)))].sort((a, b) => a - b);
  const periods = [...new Set(cs.flatMap((c) => c.schedule.map((s) => s.start)))].sort((a, b) => a - b);
  return {
    category: CATEGORY_ORDER.filter((x) => uniq("category").includes(x)),
    campus: uniq("campus").sort(),
    assessment: uniq("assessment").sort(),
    credits: uniq("credits").sort((a, b) => a - b),
    days,                                   // I-01：上课日（周一~周五）
    periods,                                // I-01：起始节次
    teachers: uniq("teacher").sort(),       // I-01：授课教师（联想用）
  };
}

/* ---------------- 心愿单 ---------------- */

export function wishlistAdd(courseId) {
  const id = Number(courseId);
  if (!db.courseById(id)) return { error: "课程不存在" };
  if (db.state.enrolledIds.includes(id)) return { error: "该课程已选中，无需加入心愿单" };
  if (db.state.wishlist.some((w) => w.courseId === id)) return { error: "已在心愿单中" };
  if (db.state.wishlist.length >= adminState.rules.maxWishlist)
    return { error: `心愿单已达上限（${adminState.rules.maxWishlist} 门），请先移除部分课程` };
  const max = Math.max(0, ...db.state.wishlist.map((w) => w.priority));
  db.state.wishlist.push({ courseId: id, priority: max + 1, addedAt: Date.now() });
  return { ok: true };
}

export function wishlistRemove(courseId) {
  const id = Number(courseId);
  const before = db.state.wishlist.length;
  db.state.wishlist = db.state.wishlist.filter((w) => w.courseId !== id);
  db.state.wishlist.forEach((w, i) => (w.priority = i + 1));
  return { ok: before !== db.state.wishlist.length };
}

export function wishlistReorder(orderedIds) {
  const map = new Map(db.state.wishlist.map((w) => [w.courseId, w]));
  const next = [];
  (orderedIds || []).forEach((cid, idx) => {
    const w = map.get(Number(cid));
    if (w) next.push({ ...w, priority: idx + 1 });
  });
  db.state.wishlist = next;
  return { ok: true };
}

export function wishlistDetail() {
  const enrolled = db.enrolled();
  return db.wishlist().map((w) => {
    const conflicts = detectConflicts(w.course, enrolled);
    return { courseId: w.courseId, priority: w.priority, course: w.course, conflict: conflicts,
      blocked: conflicts.some((c) => c.level === "hard") };
  });
}

/* ---------------- 选课受理（异步队列） ---------------- */

export function submitSelection() {
  const wish = db.wishlist();
  if (!wish.length) return { error: "心愿单为空，请先添加课程" };
  if (!adminState.rules.selectionOpen) return { error: "选课未开放（教务已暂时关闭选课通道）" };
  /* 阶段窗口门禁：教务关着开关、或当前时刻不在任何阶段内，都不受理提交。
   * 与 server-java 的 StudentService#submitSelection 保持同一套判断口径。 */
  if (!currentPhase()) {
    const nxt = nextPhase();
    return {
      error: nxt
        ? `当前不在选课阶段内，「${nxt.phaseName}」将于 ${fmtPhaseTime(nxt.startTime)} 开放`
        : "当前不在选课阶段内，请等待教务安排选课时间",
    };
  }
  const ticketId = "TK" + db.state.seq.ticket++;
  // I-05 四态：已受理 → 处理中 → 成功/失败；并给出排队位次
  const queuePos = Object.values(db.state.tickets).filter((t) => t.status === "已受理" || t.status === "处理中").length + 1;
  db.state.tickets[ticketId] = { id: ticketId, status: "已受理", queuePos, createdAt: Date.now(),
    accepted: [], rejected: [], total: wish.length };
  setTimeout(() => { const t = db.state.tickets[ticketId]; if (t && t.status === "已受理") t.status = "处理中"; }, 350);
  setTimeout(() => processTicket(ticketId), config.processDelayMs); // 模拟 MQ 异步落库
  return { ok: true, ticket: db.state.tickets[ticketId], message: "已受理，正在处理" };
}

/** 把被拒的选课记录为异常工单，供教务处理 */
function logAnomaly(type, course, reason) {
  adminState.anomalies.unshift({
    id: adminState.seq.anomaly++,
    type,
    student: db.state.user.studentNo + " " + db.state.user.name,
    courseId: course ? course.id : null,
    courseName: course ? course.name : "—",
    reason,
    time: new Date().toLocaleString("zh-CN", { hour12: false }),
    status: "pending",
  });
}

function processTicket(ticketId) {
  const t = db.state.tickets[ticketId];
  if (!t) return;
  let enrolled = db.enrolled();
  for (const w of db.wishlist()) {
    const c = w.course;
    const conflicts = detectConflicts(c, enrolled);
    if (conflicts.some((x) => x.level === "hard")) {
      const reason = conflicts.find((x) => x.level === "hard").message;
      t.rejected.push({ courseId: c.id, name: c.name, reason });
      logAnomaly("conflict", c, reason);
      continue;
    }
    if (totalCredits([c.id]) > adminState.rules.creditLimit) {
      const reason = `超出学分上限（${adminState.rules.creditLimit} 学分）`;
      t.rejected.push({ courseId: c.id, name: c.name, reason });
      logAnomaly("credit", c, reason);
      continue;
    }
    if (!db.takeSeat(c.id)) {
      const reason = "名额已满";
      t.rejected.push({ courseId: c.id, name: c.name, reason });
      logAnomaly("full", c, reason);
      continue;
    }
    db.state.enrolledIds.push(c.id);
    enrolled = db.enrolled();
    t.accepted.push({ courseId: c.id, name: c.name, reason: "选课成功" });
  }
  t.accepted.forEach((a) => wishlistRemove(a.courseId));
  t.status = t.accepted.length > 0 ? "成功" : "失败"; // I-05 终态
  t.finishedAt = Date.now();
  db.state.messages.unshift({
    id: db.state.seq.message++, type: "result",
    title: `选课结果：成功 ${t.accepted.length} 门，失败 ${t.rejected.length} 门`,
    body: t.accepted.map((a) => a.name).join("、") || "无成功项",
    time: new Date().toLocaleString("zh-CN", { hour12: false }), read: false,
  });
}

export function ticketStatus(id) {
  return db.state.tickets[id] || { error: "票据不存在" };
}

/* ---------------- 课表 / 消息 / 偏好 ---------------- */

export function timetable() {
  return {
    view: db.state.user.preferences.timetableView,
    totalCredits: totalCredits(),
    courses: db.enrolled().map((c) => ({
      id: c.id, name: c.name, teacher: c.teacher, place: c.place,
      campus: c.campus, credits: c.credits, schedule: c.schedule,
    })),
  };
}

export function setPreferences(patch) {
  Object.assign(db.state.user.preferences, patch || {});
  return db.state.user.preferences;
}

export function markMessagesRead() {
  db.state.messages.forEach((m) => (m.read = true));
  return { ok: true };
}

export const DAY_NAMES_EXPORT = DAY_NAMES;

/* ---------------- 放号订阅（I-04 / P-05-2：名额由 0 变有量时提醒） ---------------- */

export function subscribe(courseId) {
  const id = Number(courseId);
  const c = db.courseById(id);
  if (!c) return { error: "课程不存在" };
  if (c.remaining > 0) return { error: "该课程尚有名额，无需订阅" };
  if (db.state.subscriptions.includes(id)) return { error: "已订阅该课程的放号通知" };
  db.state.subscriptions.push(id);
  return { ok: true };
}

export function unsubscribe(courseId) {
  const id = Number(courseId);
  db.state.subscriptions = db.state.subscriptions.filter((x) => x !== id);
  return { ok: true };
}

export function subscriptions() {
  return db.state.subscriptions.map((id) => db.courseById(id)).filter(Boolean);
}

/** 某课程名额由 0 恢复为有量后调用：给订阅了它的账号推送「放号通知」并清除订阅 */
export function notifySubscribers(course) {
  if (!course || course.remaining <= 0) return 0;
  let sent = 0;
  for (const acc of db.allAccounts()) {
    if (!acc.subscriptions.includes(course.id)) continue;
    acc.messages.unshift({
      id: acc.seq.message++, type: "seat",
      title: `放号通知：${course.name}`,
      body: `你订阅的「${course.name}」已放出名额（现余 ${course.remaining} 个），请尽快前往抢课。`,
      time: new Date().toLocaleString("zh-CN", { hour12: false }), read: false,
    });
    acc.subscriptions = acc.subscriptions.filter((x) => x !== course.id);
    sent++;
  }
  return sent;
}

/* ---------------- 课表导出（P-06-4：.ics 日历文件） ---------------- */

const PERIOD_TIME = {
  1: ["0800", "0845"], 2: ["0855", "0940"], 3: ["1000", "1045"], 4: ["1055", "1140"],
  5: ["1200", "1245"], 6: ["1400", "1445"], 7: ["1455", "1540"], 8: ["1600", "1645"],
  9: ["1655", "1740"], 10: ["1900", "1945"], 11: ["1955", "2040"], 12: ["2050", "2135"],
};

function icsEsc(s) {
  return String(s).replace(/\\/g, "\\\\").replace(/,/g, "\\,").replace(/;/g, "\\;").replace(/\r?\n/g, "\\n");
}

export function timetableIcs() {
  const pad = (n) => String(n).padStart(2, "0");
  const base = new Date(2026, 8, 21); // 2026-09-21 周一（学期第 1 周）
  const dt = (d, hm) => `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}T${hm}00`;
  const lines = ["BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//course-platform//timetable//CN", "CALSCALE:GREGORIAN"];
  let uid = 1;
  for (const c of db.enrolled()) {
    for (const s of c.schedule) {
      const d = new Date(base);
      d.setDate(base.getDate() + (s.day - 1));
      const st = (PERIOD_TIME[s.start] || ["0800"])[0];
      const en = (PERIOD_TIME[s.end] || PERIOD_TIME[s.start] || ["", "0930"])[1];
      lines.push(
        "BEGIN:VEVENT",
        `UID:cp-${c.id}-${s.day}-${s.start}@course-platform`,
        `DTSTART:${dt(d, st)}`,
        `DTEND:${dt(d, en)}`,
        "RRULE:FREQ=WEEKLY;COUNT=16",
        `SUMMARY:${icsEsc(c.name)}`,
        `LOCATION:${icsEsc(c.campus)} ${icsEsc(c.place)}`,
        `DESCRIPTION:${icsEsc(c.teacher)}老师 · ${c.credits} 学分`,
        "END:VEVENT"
      );
      uid++;
    }
  }
  lines.push("END:VCALENDAR");
  return lines.join("\r\n");
}

/* ---------------- 多账号（I-09 / P-08-4） ---------------- */

export function me() {
  return { accountId: db.currentAccountId(), user: db.user(), accounts: db.accounts() };
}

export function switchAccount(id) {
  const acc = db.switchAccount(id);
  if (!acc) return { error: "账号不存在" };
  return { ok: true, account: acc, user: db.user() };
}

/* ---------------- 教师端 ---------------- */

function isMine(course) {
  return course && course.teacher === currentTeacher.name;
}

export function teacherProfile() {
  const courses = db.allCourses().filter(isMine);
  const totalStudents = courses.reduce((s, c) => s + c.enrolled, 0);
  const totalCapacity = courses.reduce((s, c) => s + c.capacity, 0);
  const remaining = courses.reduce((s, c) => s + c.remaining, 0);
  const fillRate = totalCapacity ? Math.round((totalStudents / totalCapacity) * 100) : 0;
  return { ...currentTeacher, courseCount: courses.length, totalStudents, totalCapacity, remaining, fillRate };
}

export function teacherCourses() {
  return db.allCourses().filter(isMine).map((c) => ({
    ...c,
    fillRate: c.capacity ? Math.round((c.enrolled / c.capacity) * 100) : 0,
    rosterCount: Math.min(c.enrolled, 30),
  }));
}

export function teacherRoster(courseId) {
  const c = db.courseById(courseId);
  if (!c) return { error: "课程不存在" };
  if (!isMine(c)) return { error: "只能查看本人授课课程的名单" };
  return {
    course: { id: c.id, name: c.name, code: c.code, place: c.place, schedule: c.schedule },
    students: genRoster(c),
  };
}

/** 课程信息维护：仅允许修改 intro / place，且仅限本人授课 */
export function teacherUpdateCourse(courseId, patch = {}) {
  const c = db.courseById(courseId);
  if (!c) return { error: "课程不存在" };
  if (!isMine(c)) return { error: "只能编辑本人授课的课程" };
  if (typeof patch.intro === "string" && patch.intro.trim()) c.intro = patch.intro.trim().slice(0, 500);
  if (typeof patch.place === "string" && patch.place.trim()) c.place = patch.place.trim().slice(0, 60);
  return { ok: true, course: c };
}

/* ---------------- 教务管理端 ---------------- */

export function adminOverview() {
  const totalCap = db.allCourses().reduce((s, c) => s + c.capacity, 0);
  const totalEnr = db.allCourses().reduce((s, c) => s + c.enrolled, 0);
  return {
    courseCount: db.allCourses().length,
    totalCapacity: totalCap,
    totalEnrolled: totalEnr,
    seatsRemaining: totalCap - totalEnr,
    fillRate: totalCap ? Math.round((totalEnr / totalCap) * 100) : 0,
    pendingAnomalies: adminState.anomalies.filter((a) => a.status === "pending").length,
    totalRequests: adminState.metrics.totalRequests,
    uptime: uptimeSec(),
    rules: adminState.rules,
  };
}

export function getRules() {
  return adminState.rules;
}

/* ---------------- 选课阶段（selection_period） ---------------- */

/** 阶段时间表：含 status / current 标记，供学生端首页与抢课专区展示 */
export function listPeriods() {
  return { items: phaseViews() };
}

/**
 * 教务端调整某阶段的起止时间。
 *
 * 改完之后 openAt 会跟着变（openAt 由阶段表推导），学生端下次轮询
 * /api/status 就能看到新的倒计时，不需要重启服务。
 */
export function updatePeriod(code, patch = {}) {
  const p = applyPeriod(String(code).toUpperCase(), patch);
  if (!p) return { error: `阶段不存在：${code}` };
  return { ok: true, phase: phaseViews().find((x) => x.code === p.phaseCode) || null, items: phaseViews() };
}

export function updateRules(patch = {}) {
  const r = adminState.rules;
  for (const k of ["selectionOpen", "allowCrossCampus", "blockOnConflict"]) if (k in patch) r[k] = !!patch[k];
  /* 学分上限与心愿单上限的取值范围不同，别共用同一个钳制区间：
   * 学分上限要允许教务配到 120 甚至更高（辅修 / 重修叠加场景），
   * 心愿单上限则是个位数级别的列表容量。 */
  const RANGE = { creditLimit: [1, 300], maxWishlist: [1, 60] };
  for (const k of ["creditLimit", "maxWishlist"]) {
    if (k in patch) {
      const v = Number(patch[k]);
      if (!Number.isNaN(v)) {
        const [lo, hi] = RANGE[k];
        r[k] = Math.max(lo, Math.min(hi, v | 0));
      }
    }
  }
  /* 落盘，否则刷新页面规则就被 loadRules() 打回默认值 */
  persistRules();
  return r;
}

export function adminMonitor() {
  const m = adminState.metrics;
  // 静态版：浏览器用 performance.memory（仅 Chromium 提供）估算堆占用
  const pm = (typeof performance !== "undefined" && performance.memory) || null;
  const usedBytes = pm ? pm.usedJSHeapSize : 0;
  return {
    startedAt: m.startedAt,
    uptime: uptimeSec(),
    node: "browser（静态版）",
    memoryMB: Math.round(usedBytes / 1048576),
    totalRequests: m.totalRequests,
    errors: m.errors,
    sseClients: m.sseClients,
    tickets: Object.keys(db.state.tickets).length,
    wishlistSize: db.state.wishlist.length,
    enrolledCount: db.state.enrolledIds.length,
    byPath: Object.entries(m.byPath).sort((a, b) => b[1] - a[1]).slice(0, 12).map(([path, count]) => ({ path, count })),
    recentTickets: Object.values(db.state.tickets).slice(-5).reverse()
      .map((t) => ({ id: t.id, status: t.status, accepted: t.accepted.length, rejected: t.rejected.length, total: t.total })),
  };
}

export function adminAnomalies(status) {
  let list = adminState.anomalies.slice();
  if (status === "pending" || status === "resolved") list = list.filter((a) => a.status === status);
  return list;
}

/** 处理异常工单：force=强制补选 / dismiss=标记已处理 */
export function resolveAnomaly(id, action) {
  const a = adminState.anomalies.find((x) => x.id === Number(id));
  if (!a) return { error: "工单不存在" };
  if (a.status === "resolved") return { error: "该工单已处理" };

  if (action === "dismiss") {
    a.status = "resolved";
    a.resolution = "已标记为处理完成";
    return { ok: true, anomaly: a };
  }

  if (action === "force") {
    if (!a.courseId) { a.status = "resolved"; a.resolution = "已强制处理"; return { ok: true, anomaly: a }; }
    const c = db.courseById(a.courseId);
    if (!c) return { error: "课程不存在" };
    if (db.state.enrolledIds.includes(c.id)) return { error: "该生已选中此课，无需补选" };
    if (c.remaining > 0) db.takeSeat(c.id); else c.enrolled += 1; // 教务强制补选，允许超出容量
    db.state.enrolledIds.push(c.id);
    a.status = "resolved";
    a.resolution = `已强制补选「${c.name}」${c.enrolled > c.capacity ? "（超出容量）" : ""}`;
    db.state.messages.unshift({
      id: db.state.seq.message++, type: "result",
      title: `教务补选成功：${c.name}`,
      body: "管理员已为你强制补选该课程，请核对课表。",
      time: new Date().toLocaleString("zh-CN", { hour12: false }), read: false,
    });
    return { ok: true, anomaly: a };
  }
  return { error: "未知操作" };
}

/** 调整课程容量（释放/缩减名额） */
export function adjustSeats(courseId, delta) {
  const c = db.courseById(courseId);
  if (!c) return { error: "课程不存在" };
  const d = Number(delta) || 0;
  if (!d) return { error: "调整量不能为 0" };
  const newCap = c.capacity + d;
  if (newCap < c.enrolled) return { error: `容量不能低于已选人数（${c.enrolled}）` };
  c.capacity = newCap;
  c.remaining = Math.max(0, c.remaining + d);
  return { ok: true, course: c };
}

export function adminReset() {
  return resetDemo();
}
