/* 前端 · 应用引导：导航、路由、全局事件 */

import { api } from "./api.js";
import { store, applyPrefs } from "./store.js";
import { icon, toast, esc } from "./ui.js";
import { register as registerCountdown, setTarget as setCountdownTarget, onPhase as onCountdownPhase } from "./countdown.js";
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
    view.innerHTML = `<div class="card"><h2>加载失败</h2><p style="color:var(--danger)">${String(e && e.message)}</p><p style="color:var(--tx-2);font-size:var(--fs-2)">演示数据内置在浏览器端（js/backend/），若持续失败请检查浏览器控制台。</p></div>`;
  }
  window.scrollTo(0, 0);
  refreshBadge();
}

/* ---------- 顶部倒计时：交给统一倒计时中心 ---------- */
let topCdDispose = null;
let topCdPhaseOff = null;
function startTopCountdown() {
  const node = document.getElementById("topCountdownText");
  if (!node) return;
  if (topCdDispose) return;              // 重复登录不再叠加订阅
  topCdDispose = registerCountdown(node, { compact: true, phaseClass: false });
  topCdPhaseOff = onCountdownPhase(({ phase }) => {
    const chip = node.closest(".countdown-chip");
    if (chip) chip.dataset.phase = phase;
  });
}
function stopTopCountdown() {
  if (topCdDispose) { topCdDispose(); topCdDispose = null; }
  if (topCdPhaseOff) { topCdPhaseOff(); topCdPhaseOff = null; }
}

/* ---------- 全局搜索（含关键词联想 P-02-3） ---------- */
function bindSearch() {
  const input = document.getElementById("globalSearch");
  const box = input.closest(".topbar-search");
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

/* ============================================================
 * 多端登录（学生 / 教师 / 教务）
 *
 * 说明：静态演示版没有真实的鉴权服务端点，因此这里在浏览器端复刻
 * 与后端 AuthService 一致的登录语义：
 *   - 三端各自独立的账号密码（与 server-java 的 sys_user 表一致）
 *   - 校验通过后写入会话，按角色装载对应导航与首屏
 *   - 退出登录清空会话并回到登录页
 * 若将来把前端接到真实后端，只需把 verifyCredentials 换成 api.login()。
 * ============================================================ */
const CREDENTIALS = {
  student: [{ username: "2023010101", password: "123456", name: "林同学" },
            { username: "2023010102", password: "123456", name: "苏同学" },
            { username: "2023010103", password: "123456", name: "何同学" }],
  teacher: [{ username: "T1005", password: "teacher123", name: "刘洋" }],
  admin:   [{ username: "admin", password: "admin123", name: "教务处管理员" }],
};

const ROLE_HINT = {
  student: "学生端 · 学号登录。演示账号：2023010101 / 123456",
  teacher: "教师端 · 工号登录。演示账号：T1005 / teacher123",
  admin: "教务端 · 管理账号登录。演示账号：admin / admin123",
};

const ROLE_LABEL = { student: "学生端", teacher: "教师端", admin: "教务端" };
const SESSION_KEY = "cp_session";

function readSession() {
  try { return JSON.parse(localStorage.getItem(SESSION_KEY) || "null"); } catch (e) { return null; }
}
function writeSession(s) {
  try {
    if (s) localStorage.setItem(SESSION_KEY, JSON.stringify(s));
    else localStorage.removeItem(SESSION_KEY);
  } catch (e) { /* 隐私模式下忽略 */ }
}

let loginRole = "student";

function setLoginRole(role) {
  loginRole = ROLE_LABEL[role] ? role : "student";
  document.querySelectorAll("#loginRoles .chip").forEach((c) => c.classList.toggle("on", c.dataset.role === loginRole));
  const hint = document.getElementById("loginHint");
  if (hint) hint.textContent = ROLE_HINT[loginRole];
  const u = document.getElementById("loginUser");
  if (u) {
    u.placeholder = loginRole === "student" ? "请输入学号，例如 2023010101"
      : loginRole === "teacher" ? "请输入工号，例如 T1005"
      : "请输入管理账号，例如 admin";
  }
  showLoginError("");
}

function showLoginError(msg) {
  const err = document.getElementById("loginErr");
  if (!err) return;
  err.textContent = msg || "";
  if (msg) { err.classList.remove("shake"); void err.offsetWidth; err.classList.add("shake"); }
}

function showLogin() {
  const el = document.getElementById("loginScreen");
  if (el) el.hidden = false;
  showLoginError("");
}

function hideLogin() {
  const el = document.getElementById("loginScreen");
  if (el) el.hidden = true;
}

function renderRoleBadge() {
  const role = store.get().role;
  const el = document.getElementById("roleBadgeText");
  if (el) el.textContent = ROLE_LABEL[role] || "未登录";
  const badge = document.getElementById("roleBadge");
  if (badge) badge.dataset.role = role;
}

/* 校验三端账号密码（演示版本地比对，语义与后端 AuthService 一致） */
function verifyCredentials(role, username, password) {
  const list = CREDENTIALS[role] || [];
  const hit = list.find((c) => c.username === username);
  if (!hit) return { error: "账号不存在，或所选端与账号不匹配" };
  if (hit.password !== password) return { error: "密码错误" };
  return { ok: true, user: hit };
}

function applySession(session) {
  store.setRole(session.role);
  store.setMe({ user: { name: session.name }, session: { role: session.role, username: session.username }, accounts: [] });
  const av = document.getElementById("userAvatar");
  const nm = document.getElementById("userName");
  if (av) av.textContent = (session.name || "?").slice(0, 1);
  if (nm) nm.textContent = session.name || "";
  const chip = document.getElementById("userChip");
  if (chip) chip.title = `${session.name || ""} · ${ROLE_LABEL[session.role] || ""}（点击进入个人中心）`;
  const lb = document.getElementById("logoutBtn");
  if (lb) lb.style.display = "inline-flex";
  renderRoleBadge();
}

async function doLogin() {
  const u = document.getElementById("loginUser");
  const p = document.getElementById("loginPass");
  const btn = document.getElementById("loginBtn");
  const username = ((u && u.value) || "").trim();
  const password = (p && p.value) || "";
  if (!username || !password) { showLoginError("请输入账号和密码"); return; }

  const r = verifyCredentials(loginRole, username, password);
  if (r.error) { showLoginError(r.error); if (p) p.select(); return; }

  if (btn) { btn.disabled = true; btn.textContent = "登录中…"; }
  const session = { role: loginRole, username, name: r.user.name, at: Date.now() };
  writeSession(session);
  applySession(session);
  hideLogin();
  if (btn) { btn.disabled = false; btn.innerHTML = "登　录"; }
  toast(`欢迎回来，${session.name}`, `${ROLE_LABEL[session.role]}已登录`, "ok", 1600);
  await bootApp();
}

async function doLogout() {
  writeSession(null);
  stopTopCountdown();
  setCountdownTarget(null);
  store.setMe(null);
  store.setRole("student");
  const view = document.getElementById("view");
  if (view) view.innerHTML = "";
  const lb = document.getElementById("logoutBtn");
  if (lb) lb.style.display = "none";
  renderRoleBadge();
  setLoginRole("student");
  const u = document.getElementById("loginUser");
  const p = document.getElementById("loginPass");
  if (u) u.value = "";
  if (p) p.value = "";
  showLogin();
  if (p) p.focus();
  toast("已退出登录", "", "ok", 1200);
}

/* ---------- 引导 ---------- */
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

  try {
    const [prefs, status] = await Promise.all([api.prefs(), api.status()]);
    store.setPrefs(prefs.data);
    setCountdownTarget(status.data.openAt);
  } catch (e) {
    applyPrefs();
    toast("演示数据加载异常", String((e && e.message) || e), "warn", 3000);
  }

  startTopCountdown();
  await loadMe();
  await render();
}

/* 首次进入：绑定导航与全局事件（只做一次，与登录态无关） */
function bindShell() {
  document.getElementById("navToggle").onclick = () => {
    const sn = document.getElementById("sidenav");
    sn.classList.toggle("open");
    document.getElementById("scrim").hidden = !sn.classList.contains("open");
    document.getElementById("scrim").onclick = closeSidenav;
  };
  bindSearch();
  window.addEventListener("hashchange", render);

  /* 移动端搜索行开合（<900px 时顶栏搜索框隐藏，改用按钮唤起） */
  const mToggle = document.getElementById("mSearchToggle");
  const mBar = document.getElementById("mSearch");
  if (mToggle && mBar) {
    mToggle.onclick = () => {
      mBar.hidden = !mBar.hidden;
      mToggle.classList.toggle("on", !mBar.hidden);
      mToggle.setAttribute("aria-expanded", String(!mBar.hidden));
      if (!mBar.hidden) document.getElementById("mSearchInput").focus();
    };
  }
}

async function boot() {
  bindShell();

  /* 登录页交互 */
  const roles = document.getElementById("loginRoles");
  if (roles) roles.addEventListener("click", (e) => {
    const c = e.target.closest(".chip");
    if (c) setLoginRole(c.dataset.role);
  });

  /* 点演示账号一键回填 */
  document.querySelectorAll(".demo-item[data-role]").forEach((b) => (b.onclick = () => {
    setLoginRole(b.dataset.role);
    document.getElementById("loginUser").value = b.dataset.user || "";
    document.getElementById("loginPass").value = b.dataset.pass || "";
    document.getElementById("loginPass").focus();
    showLoginError("");
  }));

  const loginBtn = document.getElementById("loginBtn");
  if (loginBtn) loginBtn.onclick = doLogin;
  const pass = document.getElementById("loginPass");
  if (pass) pass.addEventListener("keydown", (e) => { if (e.key === "Enter") doLogin(); });
  const user = document.getElementById("loginUser");
  if (user) user.addEventListener("keydown", (e) => { if (e.key === "Enter") { const p = document.getElementById("loginPass"); if (p) p.focus(); } });
  const logoutBtn = document.getElementById("logoutBtn");
  if (logoutBtn) logoutBtn.onclick = doLogout;

  setLoginRole("student");
  applyPrefs();

  /* 已有会话（刷新页面）直接进入；否则停在登录页 */
  const saved = readSession();
  if (saved && ROLE_LABEL[saved.role]) {
    applySession(saved);
    hideLogin();
    await bootApp();
  } else {
    renderRoleBadge();
    showLogin();
  }
}

boot();
