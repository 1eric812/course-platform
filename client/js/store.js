/* 前端 · 全局状态（轻量 store） */

/* 本地缓存的外观偏好（P-08-1）：登录态之外的纯前端表现，登录前后都保持一致体验 */
function readCachedPrefs() {
  try { return JSON.parse(localStorage.getItem("cp_prefs") || "{}") || {}; } catch (e) { return {}; }
}

const state = {
  token: localStorage.getItem("cp_token") || null, // 登录 Token
  session: null, // { username, role, realName, ... }
  role: "student", // student | teacher | admin
  prefs: Object.assign(
    { theme: "light", density: "standard", timetableView: "week", compactFilter: false, fontScale: "standard", notify: { result: true, seat: true, system: true, drop: true } },
    readCachedPrefs()
  ),
  me: null, // { accountId, user, accounts }
  courses: { items: [], total: 0, credits: 0, creditLimit: 30 },
  creditLimit: 30, // 学分上限（后端 selection_rule 拉取后覆盖；首页据此渲染）
  filters: { keyword: "", category: new Set(), campus: new Set(), assessment: new Set(), credits: new Set(), days: new Set(), periods: new Set(), teacher: "", onlyNoConflict: false, onlyAvailable: false, sort: "default" },
  wishlist: [],
  seats: new Map(), // id -> remaining
  unread: 0,
  openAt: null,
};

const listeners = new Map();

export const store = {
  get: () => state,

  set(patch) {
    Object.assign(state, patch);
    this.emit("*");
  },

  /* 订阅全部变化或某个 key */
  on(fn, key = "*") {
    if (!listeners.has(key)) listeners.set(key, new Set());
    listeners.get(key).add(fn);
    return () => listeners.get(key).delete(fn);
  },

  emit(key = "*") {
    (listeners.get(key) || []).forEach((fn) => fn(state));
    (listeners.get("*") || []).forEach((fn) => fn(state));
  },

  /* 便捷更新 */
  setCourses(c) { state.courses = c; this.emit("courses"); this.emit("*"); },
  setWishlist(w) { state.wishlist = w; this.emit("wishlist"); this.emit("*"); },
  setUnread(n) { state.unread = n; this.emit("badge"); this.emit("*"); },
  setSeats(map) { state.seats = map; this.emit("seats"); },
  setPrefs(p) {
    Object.assign(state.prefs, p);
    applyPrefs();
    /* 落本地缓存：index.html 首屏 inline 脚本据此提前设置 data-* 属性，避免深色模式白闪 */
    try {
      localStorage.setItem("cp_prefs", JSON.stringify({
        theme: state.prefs.theme,
        density: state.prefs.density,
        fontScale: state.prefs.fontScale,
      }));
    } catch (e) { /* 隐私模式下 localStorage 不可写，忽略 */ }
    this.emit("prefs"); this.emit("*");
  },
  setRole(r) { state.role = r; this.emit("role"); this.emit("*"); },
  setMe(m) { state.me = m; this.emit("me"); this.emit("*"); },
  setSession(s) { state.session = s; this.emit("session"); this.emit("*"); },
  setToken(t) { state.token = t; if (t) localStorage.setItem("cp_token", t); else localStorage.removeItem("cp_token"); },

  /* P-08-4 / I-09 会话隔离：退出或切换账号时复位全部个人相关状态，
   * 杜绝上一账号的已选、心愿单、名额缓存、个人化筛选残留（多账号串号是功能性缺陷，不能只清 token）。 */
  resetVolatile() {
    state.me = null;
    state.session = null;
    state.role = "student";
    state.courses = { items: [], total: 0, credits: 0, creditLimit: 30 };
    state.wishlist = [];
    state.seats = new Map();
    state.unread = 0;
    state.openAt = null;
    const f = state.filters;
    f.keyword = "";
    f.teacher = "";
    f.days.clear();
    f.periods.clear();
    f.onlyNoConflict = false;
    f.onlyAvailable = false;
    f.sort = "default";
    ["category", "campus", "assessment", "credits"].forEach((k) => f[k].clear());
    this.emit("badge");
    this.emit("*");
  },
};

/* V-06 / P-08-2 主题解析：支持"跟随系统"，深色需在系统外观变化时同步生效 */
export function resolveTheme(theme) {
  if (theme === "dark") return "dark";
  if (theme === "system") {
    try {
      return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    } catch (e) { return "light"; }
  }
  return "light";
}

/* 系统外观变化时，仅当用户选择"跟随系统"才重新着色 */
export function watchSystemTheme() {
  try {
    const mq = window.matchMedia("(prefers-color-scheme: dark)");
    const onSys = () => { if (state.prefs.theme === "system") applyPrefs(); };
    if (mq.addEventListener) mq.addEventListener("change", onSys); else mq.addListener(onSys);
  } catch (e) { /* 浏览器不支持时忽略，主题仍可手动切换 */ }
}

export function applyPrefs() {
  const p = state.prefs;
  document.documentElement.setAttribute("data-theme", resolveTheme(p.theme));
  document.documentElement.setAttribute("data-density", p.density === "compact" ? "compact" : p.density === "loose" ? "loose" : "standard");
  document.documentElement.setAttribute("data-font", ["small", "large", "xlarge"].includes(p.fontScale) ? p.fontScale : "standard");
}
