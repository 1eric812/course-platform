/* 前端 · 全局状态（轻量 store） */

const state = {
  role: "student", // student | teacher | admin
  prefs: { theme: "light", density: "standard", timetableView: "week", compactFilter: false, fontScale: "standard", notify: { result: true, seat: true, system: true, drop: true } },
  me: null, // { accountId, user, accounts }
  courses: { items: [], total: 0, credits: 0, creditLimit: 30 },
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
  setPrefs(p) { Object.assign(state.prefs, p); applyPrefs(); this.emit("prefs"); this.emit("*"); },
  setRole(r) { state.role = r; this.emit("role"); this.emit("*"); },
  setMe(m) { state.me = m; this.emit("me"); this.emit("*"); },
};

export function applyPrefs() {
  const p = state.prefs;
  document.documentElement.setAttribute("data-theme", p.theme === "dark" ? "dark" : "light");
  document.documentElement.setAttribute("data-density", p.density === "compact" ? "compact" : p.density === "loose" ? "loose" : "standard");
  document.documentElement.setAttribute("data-font", ["small", "large", "xlarge"].includes(p.fontScale) ? p.fontScale : "standard");
}
