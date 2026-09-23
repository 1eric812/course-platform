/* 静态版「后端」：在浏览器里运行的服务端逻辑
 * 把 server/router.js 的路由表原样搬进来，直接调用 server/service.js 的业务函数，
 * 不再走 HTTP。这样整个选课平台可以纯静态部署（GitHub Pages）。 */

import * as svc from "./service.js";
import { db, CATEGORIES, adminState } from "./data.js";
import { config } from "./config.js";

export const openAt = Date.now() + config.openDelayMs;

/* ---------- 接口清单（架构视图展示用） ---------- */
export const API_CATALOG = [
  { method: "GET", path: "/api/health", desc: "健康检查与服务状态", module: "router" },
  { method: "GET", path: "/api/status", desc: "服务器时间、选课开放时间与倒计时", module: "router" },
  { method: "GET", path: "/api/architecture", desc: "接口清单（本表）", module: "router" },
  { method: "GET", path: "/api/filters", desc: "筛选项枚举（类别/校区/考核/学分/上课日/节次/教师）", module: "service" },
  { method: "GET", path: "/api/courses", desc: "课程列表：keyword/category/campus/assessment/credits/days/periods/teacher/onlyNoConflict/onlyAvailable/sort", module: "service" },
  { method: "GET", path: "/api/courses/:id", desc: "课程详情（含四类冲突检测结果）", module: "service" },
  { method: "GET", path: "/api/me", desc: "当前账号信息 + 账号列表（多账号 I-09）", module: "service" },
  { method: "POST", path: "/api/accounts/switch", desc: "切换账号 {accountId}，页面状态完全复位", module: "service" },
  { method: "GET", path: "/api/subscriptions", desc: "放号订阅列表", module: "service" },
  { method: "POST", path: "/api/subscribe", desc: "订阅放号通知 {courseId}（I-04）", module: "service" },
  { method: "POST", path: "/api/unsubscribe", desc: "取消放号订阅 {courseId}", module: "service" },
  { method: "GET", path: "/api/timetable.ics", desc: "导出课表 .ics 日历文件（P-06-4）", module: "service" },
  { method: "GET", path: "/api/wishlist", desc: "心愿单（含志愿序与冲突预警）", module: "service" },
  { method: "POST", path: "/api/wishlist", desc: "加入心愿单 {courseId}", module: "service" },
  { method: "DELETE", path: "/api/wishlist/:id", desc: "移出心愿单", module: "service" },
  { method: "PUT", path: "/api/wishlist/reorder", desc: "志愿排序 {orderedIds}", module: "service" },
  { method: "POST", path: "/api/selection/submit", desc: "批量提交，立即返回受理票据", module: "service" },
  { method: "GET", path: "/api/selection/status/:ticket", desc: "轮询受理结果", module: "service" },
  { method: "GET", path: "/api/timetable", desc: "个人课表（周视图/列表视图）", module: "service" },
  { method: "GET", path: "/api/messages", desc: "消息中心", module: "service" },
  { method: "POST", path: "/api/messages/read", desc: "全部标记已读", module: "service" },
  { method: "GET", path: "/api/preferences", desc: "读取个性化设置", module: "service" },
  { method: "PUT", path: "/api/preferences", desc: "写入个性化设置（主题/密度/课表视图）", module: "service" },
  { method: "GET", path: "/api/teacher/me", desc: "教师档案与授课统计", module: "service" },
  { method: "GET", path: "/api/teacher/courses", desc: "我授课的课程（含满班率）", module: "service" },
  { method: "GET", path: "/api/teacher/courses/:id/roster", desc: "课程选课名单", module: "service" },
  { method: "PUT", path: "/api/teacher/courses/:id", desc: "维护课程信息（intro/place）", module: "service" },
  { method: "GET", path: "/api/admin/overview", desc: "教务总览（课程/容量/满班率/待处理工单）", module: "service" },
  { method: "GET", path: "/api/admin/rules", desc: "读取选课规则", module: "service" },
  { method: "PUT", path: "/api/admin/rules", desc: "更新选课规则（实时生效）", module: "service" },
  { method: "GET", path: "/api/admin/monitor", desc: "运行监控（请求量/内存/票据）", module: "service" },
  { method: "GET", path: "/api/admin/anomalies", desc: "异常工单列表", module: "service" },
  { method: "POST", path: "/api/admin/anomalies/:id/resolve", desc: "处理工单 {action: force|dismiss}", module: "service" },
  { method: "POST", path: "/api/admin/courses/:id/seats", desc: "调整课程容量 {delta}", module: "service" },
  { method: "POST", path: "/api/admin/reset", desc: "重置演示数据", module: "service" },
  { method: "GET", path: "/api/stream/seats", desc: "名额变化实时推送（静态版由定时器模拟）", module: "router" },
];

/* ---------- 请求计数（运行监控用） ---------- */
export function recordMetric(path) {
  const m = adminState.metrics;
  m.totalRequests += 1;
  const key = String(path).replace(/\/\d+/g, "/:id");
  m.byPath[key] = (m.byPath[key] || 0) + 1;
}

/* ---------- 名额模拟推送（含放号演示） ---------- */
let tickerStarted = false;
export function startSeatTicker(onSeats) {
  if (tickerStarted) return;
  tickerStarted = true;
  setInterval(() => {
    if (Math.random() < 0.22) { // 放号：把某门已满课程放出少量名额并触发订阅通知
      const full = db.allCourses().filter((c) => c.remaining === 0);
      if (full.length) {
        const c = db.releaseSeat(full[Math.floor(Math.random() * full.length)].id, 1 + Math.floor(Math.random() * 3));
        if (c) svc.notifySubscribers(c);
      }
    }
    const pool = db.allCourses().filter((c) => c.remaining > 0);
    if (!pool.length) {
      adminState.metrics.sseClients = 1;
      onSeats({ type: "seats", serverTime: Date.now(), openAt, seats: [] });
      return;
    }
    const n = 1 + Math.floor(Math.random() * 3);
    for (let i = 0; i < n; i++) {
      const c = pool[Math.floor(Math.random() * pool.length)];
      if (Math.random() < 0.55) db.takeSeat(c.id);
    }
    adminState.metrics.sseClients = 1;
    onSeats({
      type: "seats", serverTime: Date.now(), openAt,
      seats: db.allCourses().map((c) => ({ id: c.id, remaining: c.remaining, capacity: c.capacity })),
    });
  }, config.seatTickMs);
}

/* ---------- 路由（对应 server/router.js） ---------- */
const ok = (data) => ({ status: 200, data });
const bad = (data, status = 400) => ({ status, data });

export async function handleApi(method, pathname, query = {}, body = {}) {
  const path = pathname;
  recordMetric(path);
  const q = query;

  if (path === "/api/stream/seats") return ok({ type: "hello", serverTime: Date.now(), openAt });
  if (path === "/api/health") return ok({ ok: true, service: "course-selection", uptime: 0 });
  if (path === "/api/status") return ok({ serverTime: Date.now(), openAt, categories: CATEGORIES });
  if (path === "/api/architecture") return ok({ endpoints: API_CATALOG });
  if (path === "/api/filters") return ok(svc.filterOptions());

  if (path === "/api/courses" && method === "GET") return ok(svc.listCourses(q));

  if (path.startsWith("/api/courses/") && method === "GET") {
    const c = db.courseById(path.split("/")[3]);
    if (!c) return bad({ error: "课程不存在" }, 404);
    return ok({ ...c, conflicts: svc.detectConflicts(c, db.enrolled()), selected: db.state.enrolledIds.includes(c.id) });
  }

  if (path === "/api/wishlist" && method === "GET") return ok({ items: svc.wishlistDetail() });
  if (path === "/api/wishlist" && method === "POST") {
    const r = svc.wishlistAdd(body.courseId);
    return r.error ? bad(r) : ok(r);
  }
  if (path.startsWith("/api/wishlist/") && method === "DELETE") return ok(svc.wishlistRemove(path.split("/")[3]));
  if (path === "/api/wishlist/reorder" && method === "PUT") return ok(svc.wishlistReorder(body.orderedIds));

  if (path === "/api/selection/submit" && method === "POST") {
    const r = svc.submitSelection();
    return r.error ? bad(r) : { status: 202, data: r };
  }
  if (path.startsWith("/api/selection/status/") && method === "GET") return ok(svc.ticketStatus(path.split("/")[4]));

  if (path === "/api/timetable" && method === "GET") return ok(svc.timetable());
  if (path === "/api/timetable.ics" && method === "GET") return ok({ ics: svc.timetableIcs() });

  /* 多账号（I-09） */
  if (path === "/api/me" && method === "GET") return ok(svc.me());
  if (path === "/api/accounts/switch" && method === "POST") {
    const r = svc.switchAccount(body.accountId);
    return r.error ? bad(r) : ok(r);
  }

  /* 放号订阅（I-04） */
  if (path === "/api/subscriptions" && method === "GET") return ok({ items: svc.subscriptions() });
  if (path === "/api/subscribe" && method === "POST") {
    const r = svc.subscribe(body.courseId);
    return r.error ? bad(r) : ok(r);
  }
  if (path === "/api/unsubscribe" && method === "POST") return ok(svc.unsubscribe(body.courseId));

  if (path === "/api/messages" && method === "GET") return ok({ items: db.messages() });
  if (path === "/api/messages/read" && method === "POST") return ok(svc.markMessagesRead());
  if (path === "/api/preferences" && method === "GET") return ok(db.user().preferences);
  if (path === "/api/preferences" && method === "PUT") return ok(svc.setPreferences(body));

  /* 教师端 */
  if (path === "/api/teacher/me" && method === "GET") return ok(svc.teacherProfile());
  if (path === "/api/teacher/courses" && method === "GET") return ok({ items: svc.teacherCourses() });
  const rosterM = path.match(/^\/api\/teacher\/courses\/(\d+)\/roster$/);
  if (rosterM && method === "GET") {
    const r = svc.teacherRoster(rosterM[1]);
    return r.error ? bad(r, 403) : ok(r);
  }
  const editM = path.match(/^\/api\/teacher\/courses\/(\d+)$/);
  if (editM && method === "PUT") {
    const r = svc.teacherUpdateCourse(editM[1], body);
    return r.error ? bad(r, 403) : ok(r);
  }

  /* 教务端 */
  if (path === "/api/admin/overview" && method === "GET") return ok(svc.adminOverview());
  if (path === "/api/admin/rules" && method === "GET") return ok(svc.getRules());
  if (path === "/api/admin/rules" && method === "PUT") return ok(svc.updateRules(body));
  if (path === "/api/admin/monitor" && method === "GET") return ok(svc.adminMonitor());
  if (path === "/api/admin/anomalies" && method === "GET") return ok({ items: svc.adminAnomalies(q.status) });
  const resolveM = path.match(/^\/api\/admin\/anomalies\/(\d+)\/resolve$/);
  if (resolveM && method === "POST") {
    const r = svc.resolveAnomaly(resolveM[1], body.action);
    return r.error ? bad(r) : ok(r);
  }
  const seatsM = path.match(/^\/api\/admin\/courses\/(\d+)\/seats$/);
  if (seatsM && method === "POST") {
    const r = svc.adjustSeats(seatsM[1], body.delta);
    return r.error ? bad(r) : ok(r);
  }
  if (path === "/api/admin/reset" && method === "POST") return ok(svc.adminReset());

  return bad({ error: "接口不存在", path }, 404);
}
