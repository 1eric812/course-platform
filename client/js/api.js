/* 前端 · 通信层（ESM）
 * 所有后端请求唯一出口；记录请求日志，供「架构视图」实时展示前后端交互。 */

const log = [];
const logListeners = new Set();
const seatListeners = new Set();

/* ---- 登录 Token 注入（多端登录） ---- */
let token = localStorage.getItem("cp_token") || null;
let onUnauthorized = null; // 401 时触发（由 main.js 注册，用于跳回登录页）
export const auth = {
  getToken: () => token,
  setToken(t) { token = t || null; if (t) localStorage.setItem("cp_token", t); else localStorage.removeItem("cp_token"); },
  onUnauthorized(fn) { onUnauthorized = fn; },
};

function emitLog() {
  logListeners.forEach((fn) => fn(log.slice(0, 40)));
}

function record(entry) {
  log.unshift(entry);
  if (log.length > 60) log.pop();
  emitLog();
}

async function req(method, path, body) {
  const started = performance.now();
  const opt = { method, headers: {} };
  if (token) opt.headers["Authorization"] = "Bearer " + token;
  if (body !== undefined) {
    opt.headers["Content-Type"] = "application/json";
    opt.body = JSON.stringify(body);
  }
  let status = 0, data = null, error = null;
  try {
    const res = await fetch(path, opt);
    status = res.status;
    const text = await res.text();
    try { data = JSON.parse(text); } catch (e) { data = text; }
    if (status === 401 && data && data.code === "AUTH_REQUIRED" && onUnauthorized) onUnauthorized();
  } catch (e) {
    error = String((e && e.message) || e);
  }
  const ms = Math.round(performance.now() - started);
  record({ method, path, status, ms, at: new Date().toLocaleTimeString("zh-CN", { hour12: false }), error });
  if (error) throw new Error(error);
  return { status, data };
}

/* ---- 名额实时通道 ----
 * 首选 SSE；连续失败后降级为轮询 GET /api/courses?sort=remaining，
 * 后端恢复后再自动切回 SSE。两种情况都以 {type:"seats", seats:[{id, remaining}]} 形状回调。 */
let es = null;
let esRetry = 0;          // SSE 连续失败次数
let esRetryTimer = null;  // SSE 重连定时器
let pollTimer = null;     // 降级轮询定时器
let pollFailures = 0;
const ES_RETRY_BASE = 2000;   // 退避基数
const ES_RETRY_MAX = 30000;   // 退避上限
const ES_GIVE_UP_AT = 3;      // 连续失败 3 次 → 降级轮询
const POLL_INTERVAL = 5000;   // 降级轮询间隔

function emitSeats(payload) { seatListeners.forEach((fn) => fn(payload)); }

function logSse(note) {
  record({ method: "SSE", path: "/api/stream/seats", status: 200, ms: 0,
    at: new Date().toLocaleTimeString("zh-CN", { hour12: false }), note });
}

/* 降级轮询：拉当前余量并广播，形状与 SSE 一致 */
async function pollOnce() {
  try {
    const res = await fetch("/api/courses?sort=remaining", {
      headers: token ? { Authorization: "Bearer " + token } : {},
    });
    if (!res.ok) throw new Error("HTTP " + res.status);
    const data = await res.json();
    pollFailures = 0;
    const seats = (data.items || []).map((c) => ({ id: c.id, remaining: c.remaining }));
    if (seats.length) emitSeats({ type: "seats", seats });
  } catch (e) {
    pollFailures++;
    logSse(`降级轮询失败（第 ${pollFailures} 次）`);
  }
}

function stopStream() {
  if (es) { es.close(); es = null; }
  if (esRetryTimer) { clearTimeout(esRetryTimer); esRetryTimer = null; }
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
  esRetry = 0;
  pollFailures = 0;
}

function startPolling() {
  if (pollTimer) return;
  logSse("SSE 不可用，已降级为每 5 秒轮询名额");
  pollOnce();
  pollTimer = setInterval(pollOnce, POLL_INTERVAL);
}

/* 轮询期间定期试探 SSE 是否恢复 */
function probeSse() {
  if (es || esRetryTimer) return;
  if (!seatListeners.size) return;
  const probe = new EventSource("/api/stream/seats?token=" + encodeURIComponent(token || ""));
  probe.onopen = () => {
    probe.close();
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
    logSse("后端已恢复，切回 SSE 实时推送");
    startStream();
  };
  probe.onerror = () => probe.close();
}

function startStream() {
  if (es || !seatListeners.size) return;
  es = new EventSource("/api/stream/seats?token=" + encodeURIComponent(token || ""));
  es.onopen = () => {
    if (esRetry > 0) logSse("SSE 连接已恢复");
    esRetry = 0;
    if (pollTimer) { clearInterval(pollTimer); pollTimer = null; }
  };
  es.onmessage = (ev) => {
    try {
      const payload = JSON.parse(ev.data);
      logSse(payload.type === "seats" ? `名额推送 ${payload.seats.length} 条` : "连接已建立");
      emitSeats(payload);
    } catch (e) { /* 忽略异常帧 */ }
  };
  es.onerror = () => {
    if (es) { es.close(); es = null; }
    if (!seatListeners.size) return;
    esRetry++;
    if (esRetry >= ES_GIVE_UP_AT) { startPolling(); probeSse(); return; }
    const wait = Math.min(ES_RETRY_BASE * 2 ** (esRetry - 1), ES_RETRY_MAX);
    esRetryTimer = setTimeout(() => { esRetryTimer = null; startStream(); }, wait);
  };
}

export const api = {
  /* 令牌读取：window.open 触发的下载类接口（ICS / CSV）无法携带请求头，改用 ?token= */
  getToken: () => token,

  /* 日志订阅（架构视图） */
  onLog(fn) { logListeners.add(fn); fn(log.slice(0, 40)); return () => logListeners.delete(fn); },

  /* 名额推送订阅（重复订阅前自动退订旧的；监听归零时关闭底层连接） */
  onSeats(fn) {
    seatListeners.add(fn);
    if (seatListeners.size === 1) startStream();
    return () => {
      seatListeners.delete(fn);
      if (!seatListeners.size) stopStream();
    };
  },

  /* 接口 */
  /* 多端登录 */
  login: (username, password, role) => req("POST", "/api/auth/login", { username, password, role }),
  logout: () => req("POST", "/api/auth/logout", {}),

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

  /* 当前登录身份 */
  me: () => req("GET", "/api/me"),

  /* 放号订阅（I-04） */
  subscriptions: () => req("GET", "/api/subscriptions"),
  subscribe: (courseId) => req("POST", "/api/subscribe", { courseId }),
  unsubscribe: (courseId) => req("POST", "/api/unsubscribe", { courseId }),
  /* 课表导出（P-06-4） */
  exportIcs: () => window.open("/api/timetable.ics?token=" + encodeURIComponent(token || ""), "_blank"),

  /* 学生端 · 学业闭环（学期 / 成绩 / 学分 / 选课记录 / 公告 / 历史课表） */
  studentScores: (semesterId) => req("GET", "/api/student/scores" + (semesterId ? "?semesterId=" + semesterId : "")),
  studentScoreHistory: () => req("GET", "/api/student/score-history"),
  studentCredits: () => req("GET", "/api/student/credits"),
  studentRecords: (semesterId) => req("GET", "/api/student/records" + (semesterId ? "?semesterId=" + semesterId : "")),
  studentAnnouncements: () => req("GET", "/api/student/announcements"),
  studentTimetables: (semesterId) => req("GET", "/api/student/timetables" + (semesterId ? "?semesterId=" + semesterId : "")),

  /* 教师端 */
  teacherMe: () => req("GET", "/api/teacher/me"),
  teacherCourses: () => req("GET", "/api/teacher/courses"),
  roster: (id) => req("GET", `/api/teacher/courses/${id}/roster`),
  updateCourse: (id, patch) => req("PUT", `/api/teacher/courses/${id}`, patch),

  /* 教师端 · 学业闭环（概览 / 成绩录入 / 统计 / 挂科 / 课表） */
  teacherOverview: () => req("GET", "/api/teacher/overview"),
  teacherCourseScores: (courseId) => req("GET", `/api/teacher/courses/${courseId}/scores`),
  teacherSaveScores: (courseId, items) => req("PUT", `/api/teacher/courses/${courseId}/scores`, { items }),
  teacherCourseStats: (courseId) => req("GET", `/api/teacher/courses/${courseId}/stats`),
  teacherFailures: (courseId) => req("GET", `/api/teacher/courses/${courseId}/failures`),
  teacherTimetable: () => req("GET", "/api/teacher/timetable"),

  /* 教务管理端 */
  adminOverview: () => req("GET", "/api/admin/overview"),
  adminRules: () => req("GET", "/api/admin/rules"),
  setAdminRules: (patch) => req("PUT", "/api/admin/rules", patch),
  /* 调整选课阶段起止时间（改完学生端倒计时立刻跟随，无需重启服务） */
  setPeriod: (code, patch) => req("PUT", `/api/admin/periods/${code}`, patch),
  adminMonitor: () => req("GET", "/api/admin/monitor"),
  adminAnomalies: (status) => req("GET", "/api/admin/anomalies" + (status ? "?status=" + status : "")),
  resolveAnomaly: (id, action) => req("POST", `/api/admin/anomalies/${id}/resolve`, { action }),
  adjustSeats: (id, delta) => req("POST", `/api/admin/courses/${id}/seats`, { delta }),
  adminReset: () => req("POST", "/api/admin/reset", {}),

  /* 教务端 · 学业闭环（学期 / 培养方案 / 课程 / 成绩审核 / 学分结算 / 毕业统计 / 公告 / 日志） */
  adminSemesters: () => req("GET", "/api/admin/semesters"),
  adminCreateSemester: (body) => req("POST", "/api/admin/semesters", body),
  adminUpdateSemester: (id, body) => req("PUT", `/api/admin/semesters/${id}`, body),
  adminArchiveSemester: (id) => req("POST", `/api/admin/semesters/${id}/archive`, {}),
  adminTrainingPlans: () => req("GET", "/api/admin/training-plans"),
  adminCreateTrainingPlan: (body) => req("POST", "/api/admin/training-plans", body),
  adminUpdateTrainingPlan: (id, body) => req("PUT", `/api/admin/training-plans/${id}`, body),
  adminCourses: () => req("GET", "/api/admin/courses"),
  adminCreateCourse: (body) => req("POST", "/api/admin/courses", body),
  adminUpdateCourse: (id, body) => req("PUT", `/api/admin/courses/${id}`, body),
  adminScores: (semesterId, status) => {
    const p = new URLSearchParams();
    if (semesterId) p.set("semesterId", semesterId);
    if (status) p.set("status", status);
    const s = p.toString();
    return req("GET", "/api/admin/scores" + (s ? "?" + s : ""));
  },
  adminAuditScore: (id, action) => req("PUT", `/api/admin/scores/${id}/audit`, { action }),
  adminCreditsSettle: (semesterId) => req("POST", "/api/admin/credits/settle", { semesterId }),
  adminGraduation: () => req("GET", "/api/admin/graduation"),
  adminAnnouncements: () => req("GET", "/api/admin/announcements"),
  adminCreateAnnouncement: (body) => req("POST", "/api/admin/announcements", body),
  adminUpdateAnnouncement: (id, body) => req("PUT", `/api/admin/announcements/${id}`, body),
  adminLogs: () => req("GET", "/api/admin/logs"),
};
