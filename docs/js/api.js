/* 静态版 · 通信层
 * 接口与原本的 fetch 版完全一致，但请求不再发往 HTTP 服务，
 * 而是直接调用内置的浏览器端「后端」（js/backend/server.js）。
 * 这样整套选课平台可以零依赖部署在 GitHub Pages 这类纯静态托管上。 */

import { handleApi, startSeatTicker, openAt, currentOpenAt } from "./backend/server.js";

const log = [];
const logListeners = new Set();
const seatListeners = new Set();

function emitLog() {
  logListeners.forEach((fn) => fn(log.slice(0, 40)));
}

function record(entry) {
  log.unshift(entry);
  if (log.length > 60) log.pop();
  emitLog();
}

let tickerOn = false;
function ensureTicker() {
  if (tickerOn) return;
  tickerOn = true;
  startSeatTicker((payload) => {
    record({
      method: "SSE", path: "/api/stream/seats", status: 200, ms: 0,
      at: new Date().toLocaleTimeString("zh-CN", { hour12: false }),
      note: `名额推送 ${payload.seats.length} 条`,
    });
    seatListeners.forEach((fn) => fn(payload));
  });
}

async function req(method, rawPath, body) {
  const started = performance.now();
  const [pathname, search = ""] = String(rawPath).split("?");
  const query = Object.fromEntries(new URLSearchParams(search));
  let res, error = null;
  try {
    res = await handleApi(method, pathname, query, body === undefined ? {} : body);
  } catch (e) {
    error = String((e && e.message) || e);
    res = { status: 500, data: { error } };
  }
  record({
    method, path: rawPath, status: res.status,
    ms: Math.round(performance.now() - started),
    at: new Date().toLocaleTimeString("zh-CN", { hour12: false }),
    error,
  });
  return { status: res.status, data: res.data };
}

export const api = {
  onLog(fn) { logListeners.add(fn); fn(log.slice(0, 40)); return () => logListeners.delete(fn); },
  onSeats(fn) { seatListeners.add(fn); ensureTicker(); return () => seatListeners.delete(fn); },

  health: () => req("GET", "/api/health"),
  status: () => req("GET", "/api/status"),
  /* 选课阶段时间表（selection_period 表驱动，倒计时锚点来源） */
  periods: () => req("GET", "/api/periods"),
  architecture: () => req("GET", "/api/architecture"),
  filters: () => req("GET", "/api/filters"),
  courses: (params) => req("GET", "/api/courses" + (params ? "?" + params : "")),
  course: (id) => req("GET", "/api/courses/" + id),
  timetable: () => req("GET", "/api/timetable"),
  messages: () => req("GET", "/api/messages"),
  readMessages: () => req("POST", "/api/messages/read", {}),
  wishlist: () => req("GET", "/api/wishlist"),
  wishlistAdd: (courseId) => req("POST", "/api/wishlist", { courseId }),
  wishlistRemove: (courseId) => req("DELETE", "/api/wishlist/" + courseId),
  wishlistReorder: (orderedIds) => req("PUT", "/api/wishlist/reorder", { orderedIds }),
  submit: () => req("POST", "/api/selection/submit", {}),
  ticket: (id) => req("GET", "/api/selection/status/" + id),
  prefs: () => req("GET", "/api/preferences"),
  setPrefs: (patch) => req("PUT", "/api/preferences", patch),

  /* 多账号（I-09） */
  me: () => req("GET", "/api/me"),
  switchAccount: (accountId) => req("POST", "/api/accounts/switch", { accountId }),

  /* 放号订阅（I-04） */
  subscriptions: () => req("GET", "/api/subscriptions"),
  subscribe: (courseId) => req("POST", "/api/subscribe", { courseId }),
  unsubscribe: (courseId) => req("POST", "/api/unsubscribe", { courseId }),

  /* 课表导出（静态版用 Blob 下载 .ics） */
  exportIcs: async () => {
    const r = await req("GET", "/api/timetable.ics");
    const ics = (r.data && r.data.ics) || "";
    const blob = new Blob([ics], { type: "text/calendar;charset=utf-8" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = "timetable.ics";
    a.click();
    URL.revokeObjectURL(a.href);
  },

  teacherMe: () => req("GET", "/api/teacher/me"),
  teacherCourses: () => req("GET", "/api/teacher/courses"),
  roster: (id) => req("GET", `/api/teacher/courses/${id}/roster`),
  updateCourse: (id, patch) => req("PUT", `/api/teacher/courses/${id}`, patch),

  adminOverview: () => req("GET", "/api/admin/overview"),
  adminRules: () => req("GET", "/api/admin/rules"),
  setAdminRules: (patch) => req("PUT", "/api/admin/rules", patch),
  setPeriod: (code, patch) => req("PUT", `/api/admin/periods/${code}`, patch),
  adminMonitor: () => req("GET", "/api/admin/monitor"),
  adminAnomalies: (status) => req("GET", "/api/admin/anomalies" + (status ? "?status=" + status : "")),
  resolveAnomaly: (id, action) => req("POST", `/api/admin/anomalies/${id}/resolve`, { action }),
  adjustSeats: (id, delta) => req("POST", `/api/admin/courses/${id}/seats`, { delta }),
  adminReset: () => req("POST", "/api/admin/reset", {}),
};

/* openAt 兼容导出（历史引用）；新代码应调用 currentOpenAt() 取实时值，
 * 因为它由 selection_period 阶段表推导，会随教务调整而变化。 */
export { openAt, currentOpenAt };
