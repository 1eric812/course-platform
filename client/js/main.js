/* 前端 · 应用引导：导航、路由、全局事件 */

import { api, auth } from "./api.js";
import { store, applyPrefs } from "./store.js";
import { icon, toast, esc } from "./ui.js";
import {
  renderDashboard, renderCourses, renderWishlist, renderFlash,
  renderTimetable, renderMessages, renderSettings, renderArch, renderTeacher,
  renderAdminDashboard, renderAdminRules, renderAdminMonitor, renderAdminAnomalies,
  refreshBadge, clearViewTimers,
} from "./views.js";

const NAV_STUDENT = [
  { key: "home", label: "首页", icon: "home" },
  { key: "courses", label: "课程中心", icon: "courses" },
  { key: "wishlist", label: "心愿单", icon: "heart", badge: "wishlist" },
  { key: "flash", label: "抢课专区", icon: "bolt" },
  { key: "timetable", label: "我的课表", icon: "calendar" },
  { key: "messages", label: "消息中心", icon: "bell", badge: "messages" },
  { key: "settings", label: "个人中心", icon: "settings" },
];

const NAV_TEACHER = [
  { key: "teacher", label: "教师工作台", icon: "courses" },
  { key: "messages", label: "消息中心", icon: "bell", badge: "messages" },
  { key: "settings", label: "个人中心", icon: "settings" },
];

const NAV_ADMIN = [
  { key: "admin", label: "教务工作台", icon: "home" },
  { key: "adminRules", label: "规则配置", icon: "settings" },
  { key: "adminMonitor", label: "运行监控", icon: "arch" },
  { key: "adminAnomalies", label: "异常处理", icon: "bell", badge: "anomalies" },
  { key: "arch", label: "架构视图", icon: "arch" },
];

const navFor = (role) => (role === "teacher" ? NAV_TEACHER : role === "admin" ? NAV_ADMIN : NAV_STUDENT);
const homeFor = (role) => (role === "teacher" ? "teacher" : role === "admin" ? "admin" : "home");

const ROUTES = {
  home: renderDashboard,
  courses: renderCourses,
  wishlist: renderWishlist,
  flash: renderFlash,
  timetable: renderTimetable,
  messages: renderMessages,
  settings: renderSettings,
  arch: renderArch,
  teacher: renderTeacher,
  admin: renderAdminDashboard,
  adminRules: renderAdminRules,
  adminMonitor: renderAdminMonitor,
  adminAnomalies: renderAdminAnomalies,
};

const TABBAR_KEYS = ["home", "courses", "wishlist", "timetable"];

/* 后端为 Spring Boot（server-java/），由它一并托管本前端。
 * 启动命令集中于此，便于后续变更时一处修改。 */
const BACKEND_HINT = "请确认后端已启动：cd server-java && mvn clean package -DskipTests，"
  + "再执行 java -Dserver.port=8080 -jar target/course-platform-server-1.0.0.jar";

function currentRoute() {
  const role = store.get().role;
  const h = location.hash.replace(/^#\//, "") || homeFor(role);
  const valid = navFor(role).some((n) => n.key === h);
  return valid ? h : homeFor(role);
}

function buildNav() {
  const role = store.get().role;
  const nav = navFor(role);
  const navList = document.getElementById("navList");
  navList.innerHTML = nav.map(
    (n) => `<li><button class="nav-item" data-key="${n.key}">${icon(n.icon)}<span>${n.label}</span>${n.badge ? `<span class="nav-badge" data-badge="${n.badge}" style="display:none">0</span>` : ""}</button></li>`
  ).join("");
  navList.querySelectorAll("[data-key]").forEach((b) => (b.onclick = () => { location.hash = "#/" + b.dataset.key; closeSidenav(); }));

  const tabKeys = role === "teacher" ? ["teacher", "messages", "settings"]
    : role === "admin" ? ["admin", "adminRules", "adminMonitor", "adminAnomalies"]
    : TABBAR_KEYS;
  const tabbar = document.getElementById("tabbar");
  tabbar.innerHTML = tabKeys.map((k) => {
    const n = nav.find((x) => x.key === k);
    return `<button data-key="${k}">${icon(n.icon)}<span>${n.label}</span></button>`;
  }).join("");
  tabbar.querySelectorAll("[data-key]").forEach((b) => (b.onclick = () => (location.hash = "#/" + b.dataset.key)));
}

function syncActive() {
  const r = currentRoute();
  document.querySelectorAll(".nav-item").forEach((b) => b.classList.toggle("active", b.dataset.key === r));
  document.querySelectorAll("#tabbar button").forEach((b) => b.classList.toggle("active", b.dataset.key === r));
}

function closeSidenav() {
  document.getElementById("sidenav").classList.remove("open");
  document.getElementById("scrim").hidden = true;
}

async function render() {
  clearViewTimers();
  const r = currentRoute();
  syncActive();
  const view = document.getElementById("view");
  view.innerHTML = '<div style="padding:20px">' + '<div class="skeleton" style="width:30%"></div>'.repeat(3) + "</div>";
  try {
    await ROUTES[r](view);
  } catch (e) {
    view.innerHTML = `<div class="card"><h2>加载失败</h2><p style="color:var(--danger)">${esc(String(e && e.message))}</p><p class="hint-text">${esc(BACKEND_HINT)}</p></div>`;
  }
  window.scrollTo(0, 0);
  refreshBadge();
}

/* ---------- 顶部倒计时（模块级守卫：重复登录不再叠加定时器） ---------- */
let topTimer = null;
function startTopCountdown() {
  if (topTimer) return;
  topTimer = setInterval(() => {
    const node = document.getElementById("topCountdownText");
    const openAt = store.get().openAt;
    if (!node || !openAt) return;
    const diff = openAt - Date.now();
    if (diff <= 0) { node.textContent = "已开放"; return; }
    const s = Math.floor(diff / 1000);
    node.textContent = `${String(Math.floor(s / 60)).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`;
  }, 1000);
}

/* ---------- 全局搜索（含关键词联想 P-02-3；桌面输入框与移动端搜索行共用） ---------- */
function bindSearchInput(input) {
  const box = input.closest(".topbar-search, .m-search");
  let sug = null;
  let t = null;

  const hideSug = () => { if (sug) { sug.remove(); sug = null; } };
  const showSug = (items) => {
    hideSug();
    sug = document.createElement("div");
    sug.className = "suggest";
    sug.innerHTML = items.length
      ? items.map((c) => `<button class="suggest-item" data-name="${esc(c.name)}"><span class="s-name">${esc(c.name)}</span><span class="s-meta">${esc(c.code)} · ${esc(c.teacher)}</span></button>`).join("")
      : `<div class="suggest-empty">没有匹配的课程，试试其他关键词</div>`;
    box.appendChild(sug);
    sug.querySelectorAll("[data-name]").forEach((b) => (b.onclick = () => {
      input.value = b.dataset.name;
      store.get().filters.keyword = b.dataset.name;
      const other = input.id === "globalSearch" ? document.getElementById("mSearchInput") : document.getElementById("globalSearch");
      if (other) other.value = b.dataset.name;
      hideSug();
      if (currentRoute() !== "courses") location.hash = "#/courses"; else render();
    }));
  };

  input.addEventListener("input", () => {
    clearTimeout(t);
    const v = input.value.trim();
    t = setTimeout(async () => {
      if (v.length >= 2) {
        try { const r = await api.courses("keyword=" + encodeURIComponent(v)); showSug(r.data.items.slice(0, 6)); } catch (e) { /* 忽略联想失败 */ }
      } else hideSug();
      store.get().filters.keyword = v;
      if (currentRoute() !== "courses") location.hash = "#/courses"; else render();
    }, 250);
  });
  input.addEventListener("blur", () => setTimeout(hideSug, 150));
}

let searchBound = false; // bootApp 可能多次进入（每次登录），事件只绑一次
function bindSearch() {
  if (searchBound) return;
  searchBound = true;
  bindSearchInput(document.getElementById("globalSearch"));
  bindSearchInput(document.getElementById("mSearchInput"));

  /* 移动端搜索行开合 */
  const mBar = document.getElementById("mSearch");
  const mToggle = document.getElementById("mSearchToggle");
  if (mToggle && mBar) {
    mToggle.onclick = () => {
      mBar.hidden = !mBar.hidden;
      mToggle.classList.toggle("on", !mBar.hidden);
      mToggle.setAttribute("aria-expanded", String(!mBar.hidden));
      if (!mBar.hidden) document.getElementById("mSearchInput").focus();
    };
  }
}

/* ---------- 当前账号（I-09） ---------- */
async function loadMe() {
  try {
    const { data } = await api.me();
    store.setMe(data);
    const u = data.user || {};
    const av = document.getElementById("userAvatar");
    const nm = document.getElementById("userName");
    if (av) av.textContent = (u.name || "?").slice(0, 1);
    if (nm) nm.textContent = u.name || "";
    document.getElementById("userChip").title = `${u.name || ""} · ${u.grade || ""} ${u.major || ""}（点击进入个人中心）`;
  } catch (e) { /* 忽略 */ }
}

/* ---------- 引导 ---------- */

/* ===== 多端登录（学生 / 教师 / 教务，密码存于本地 MySQL） ===== */
const ROLE_HINT = {
  STUDENT: "学生端 · 学号登录。示例：2023010101 / 2023010102 / 2023010103，密码 123456",
  TEACHER: "教师端 · 工号登录。示例：T1001 ~ T1038，密码 teacher123（演示教师刘洋 = T1005）",
  ADMIN: "教务端 · 账号 admin，密码 admin123",
};
let loginRole = "STUDENT";

function setLoginRole(role) {
  loginRole = role;
  document.querySelectorAll("#loginRoles .chip").forEach((c) => c.classList.toggle("on", c.dataset.role === role));
  const hint = document.getElementById("loginHint");
  if (hint) hint.textContent = ROLE_HINT[role];
}

function showLogin() {
  document.getElementById("loginScreen").hidden = false;
  const err = document.getElementById("loginErr");
  if (err) err.textContent = "";
}
function hideLogin() { document.getElementById("loginScreen").hidden = true; }

function renderRoleBadge() {
  const role = store.get().role;
  const label = role === "teacher" ? "教师端" : role === "admin" ? "教务端" : "学生端";
  const el = document.getElementById("roleBadgeText");
  if (el) el.textContent = label;
  const lb = document.getElementById("logoutBtn");
  if (lb) lb.style.display = store.get().token ? "inline-flex" : "none";
}

async function doLogin() {
  const u = document.getElementById("loginUser").value.trim();
  const p = document.getElementById("loginPass").value;
  const err = document.getElementById("loginErr");
  if (!u || !p) { err.textContent = "请输入账号和密码"; return; }
  const btn = document.getElementById("loginBtn");
  btn.disabled = true;
  try {
    const { status, data } = await api.login(u, p, loginRole);
    if (status !== 200) {
      err.textContent = (data && data.error) || "登录失败";
      btn.disabled = false;
      return;
    }
    auth.setToken(data.token);
    store.setToken(data.token);
    const s = data.session || {};
    const roleMap = { STUDENT: "student", TEACHER: "teacher", ADMIN: "admin" };
    store.setSession(s);
    store.setRole(roleMap[s.role] || "student");
    renderRoleBadge();
    hideLogin();
    await bootApp();
  } catch (e) {
    err.textContent = "无法连接后端。" + BACKEND_HINT;
    btn.disabled = false;
  }
}

async function doLogout() {
  try { await api.logout(); } catch (e) { /* 忽略 */ }
  auth.setToken(null);
  store.setToken(null);
  store.setSession(null);
  document.getElementById("view").innerHTML = "";
  store.setRole("student");
  renderRoleBadge();
  setLoginRole("STUDENT");
  document.getElementById("loginUser").value = "";
  document.getElementById("loginPass").value = "";
  showLogin();
}

async function boot() {
  // 会话失效（401）时自动回到登录页
  auth.onUnauthorized(() => {
    if (!store.get().token) return;
    auth.setToken(null); store.setToken(null); store.setSession(null);
    document.getElementById("view").innerHTML = "";
    store.setRole("student");
    renderRoleBadge();
    showLogin();
    toast("登录已过期，请重新登录", "", "warn", 2500);
  });

  // 登录页交互
  document.getElementById("loginRoles").addEventListener("click", (e) => {
    const c = e.target.closest(".chip");
    if (c) setLoginRole(c.dataset.role);
  });
  document.getElementById("loginBtn").onclick = doLogin;
  document.getElementById("loginPass").addEventListener("keydown", (e) => { if (e.key === "Enter") doLogin(); });
  document.getElementById("logoutBtn").onclick = doLogout;

  setLoginRole("STUDENT");
  renderRoleBadge();
  if (!store.get().token) { showLogin(); return; } // 未登录 → 停在登录页
  await bootApp();
}

async function bootApp() {
  buildNav();

  document.getElementById("navToggle").onclick = () => {
    const sn = document.getElementById("sidenav");
    sn.classList.toggle("open");
    document.getElementById("scrim").hidden = !sn.classList.contains("open");
    document.getElementById("scrim").onclick = closeSidenav;
  };

  document.getElementById("userChip").onclick = () => (location.hash = "#/settings");

  document.getElementById("themeToggle").onclick = async () => {
    const next = store.get().prefs.theme === "dark" ? "light" : "dark";
    store.setPrefs({ theme: next });
    await api.setPrefs({ theme: next });
    toast(next === "dark" ? "已切换深色模式" : "已切换浅色模式", "", "ok", 1200);
  };

  bindSearch();
  window.addEventListener("hashchange", render);

  try {
    const [prefs, status] = await Promise.all([api.prefs(), api.status()]);
    store.setPrefs(prefs.data);
    store.get().openAt = status.data.openAt;
  } catch (e) {
    applyPrefs();
    toast("无法连接后端", BACKEND_HINT, "danger", 4000);
  }

  startTopCountdown();
  await loadMe();
  await render();
}

boot();
