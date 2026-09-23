/* 前端 · 应用引导：导航、路由、全局事件 */

import { api, auth } from "./api.js";
import { store, applyPrefs, resolveTheme, watchSystemTheme } from "./store.js";
import { icon, toast, esc, confirmDialog } from "./ui.js";
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
  /* 个人中心对所有角色开放（教师 / 教务也需要退出登录与偏好设置入口） */
  const valid = h === "settings" || navFor(role).some((n) => n.key === h);
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
  document.body.classList.remove("drawer-lock"); // 路由切换时释放移动端抽屉的滚动锁（A-03/A-05）
  closeUserMenu();
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
    document.getElementById("userChip").title = `${u.name || ""} · ${u.grade || ""} ${u.major || ""}（点击查看身份与切换）`;
    renderUserMenu(data);
  } catch (e) { /* 忽略 */ }
}

/* ---------- 当前账号下拉（P-08-4：身份概览 / 多身份切换 / 快捷设置） ---------- */
let menuOpen = false;

function closeUserMenu() {
  const menu = document.getElementById("userMenu");
  const chip = document.getElementById("userChip");
  if (!menu || menu.hidden) return;
  menu.hidden = true;
  menuOpen = false;
  if (chip) chip.setAttribute("aria-expanded", "false");
}

function renderUserMenu(data) {
  const menu = document.getElementById("userMenu");
  if (!menu) return;
  const u = (data && data.user) || {};
  const ssn = (data && data.session) || store.get().session || {};
  const accounts = Array.isArray(data && data.accounts) ? data.accounts : [];
  const roleLabel = { STUDENT: "学生端", TEACHER: "教师端", ADMIN: "教务端" }[ssn.role] || "已登录";
  const ident = ssn.role === "TEACHER" ? ssn.teacherNo : ssn.role === "ADMIN" ? ssn.username : ssn.studentNo;
  const initials = String(u.name || ssn.realName || ssn.username || "?").slice(0, 1);
  const dark = store.get().prefs.theme === "dark";

  menu.innerHTML = `
    <div class="um-head">
      <span class="um-av">${esc(initials)}</span>
      <div class="um-id">
        <div class="um-name">${esc(u.name || ssn.realName || ssn.username || "—")}</div>
        <div class="um-sub">${esc(roleLabel)}${ident ? " · " + esc(ident) : ""}</div>
      </div>
    </div>
    <button class="um-item" type="button" data-act="settings">${icon("user", 18)} 个人中心与偏好设置</button>
    <button class="um-item" type="button" data-act="theme">${icon(dark ? "sun" : "moon", 18)} ${dark ? "切换浅色模式" : "切换深色模式"}</button>
    ${accounts.length > 1 ? `
      <div class="um-sep"></div>
      <div class="um-label">我的账号 · ${accounts.length} 个在读身份</div>
      ${accounts.map((a, i) => `
        <div class="um-acct ${a.current ? "cur" : ""}">
          <span class="a-av">${esc(String(a.name || "?").slice(0, 1))}</span>
          <div class="a-meta"><b>${esc(a.name || "—")}</b><span>${esc(a.studentNo || "")} · 已选 ${Number(a.enrolledCount || 0)} 门</span></div>
          ${a.current ? `<span class="tag ok">当前</span>` : `<button class="btn sm" type="button" data-switch="${i}">切换</button>`}
        </div>`).join("")}` : ""}
    <div class="um-sep"></div>
    <button class="um-item danger" type="button" data-act="logout">${icon("logout", 18)} 退出登录</button>`;

  const q = (sel) => menu.querySelector(sel);
  q('[data-act="settings"]').onclick = () => { closeUserMenu(); location.hash = "#/settings"; };
  q('[data-act="theme"]').onclick = () => { closeUserMenu(); toggleTheme(); };
  q('[data-act="logout"]').onclick = () => { closeUserMenu(); doLogout(); };
  menu.querySelectorAll("[data-switch]").forEach((b) => (b.onclick = () => {
    closeUserMenu();
    switchIdentity(accounts[Number(b.dataset.switch)]);
  }));
}

function toggleUserMenu() {
  const menu = document.getElementById("userMenu");
  const chip = document.getElementById("userChip");
  if (!menu) return;
  if (menu.hidden) {
    menu.hidden = false;
    menuOpen = true;
    if (chip) chip.setAttribute("aria-expanded", "true");
  } else closeUserMenu();
}

/* 多身份切换（P-08-4）：不做静默换号，退出会话后回到登录页由用户重新输密码
 * 原因：学生/教师/教务分端鉴权，token 与会话一一对应，避免跨身份串用会话。 */
async function switchIdentity(acct) {
  if (!acct) return;
  const label = `${acct.name || "该身份"}${acct.studentNo ? "（" + acct.studentNo + "）" : ""}`;
  const ok = await confirmDialog("切换身份", `将退出当前登录，并用 ${esc(label)} 重新登录（需重新输入密码）。是否继续？`);
  if (!ok) return;
  try { await api.logout(); } catch (e) { /* token 可能已失效 */ }
  auth.setToken(null);
  store.setToken(null);
  store.setSession(null);
  try { sessionStorage.setItem("cp_prefill", acct.studentNo || ""); } catch (e) { /* 忽略 */ }
  location.reload();
}

/* 身份切换后回到登录页：回填学号并聚焦密码框，减少一次输入 */
function restorePendingLogin() {
  let prefill = "";
  try { prefill = sessionStorage.getItem("cp_prefill") || ""; sessionStorage.removeItem("cp_prefill"); } catch (e) { return; }
  if (!prefill || store.get().token) return;
  document.getElementById("loginUser").value = prefill;
  const hint = document.getElementById("loginHint");
  if (hint) hint.textContent = `已切换身份 · ${prefill}，请输入密码完成登录`;
  const pass = document.getElementById("loginPass");
  if (pass) pass.focus();
}

/* ---------- 主题切换（顶部按钮与账号下拉共用） ----------
 * 顶部按钮为"快捷反转"：按当前实际着色在浅色/深色之间切换；
 * "跟随系统"作为第三种模式在个人中心选择（V-06 / P-08-2）。 */
function syncThemeLabel() {
  const dark = resolveTheme(store.get().prefs.theme) === "dark";
  const t = document.getElementById("themeToggleText");
  if (t) t.textContent = dark ? "浅色" : "深色";
  const btn = document.getElementById("themeToggle");
  if (btn) btn.setAttribute("aria-label", dark ? "切换到浅色模式" : "切换到深色模式");
}

async function toggleTheme() {
  const next = resolveTheme(store.get().prefs.theme) === "dark" ? "light" : "dark";
  store.setPrefs({ theme: next });
  syncThemeLabel();
  toast(next === "dark" ? "已切换深色模式" : "已切换浅色模式", "", "ok", 1200);
  try { await api.setPrefs({ theme: next }); } catch (e) { /* 后端不可用时仅本地生效，避免打断操作 */ }
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

/* A-07 登录前后上下文保持：被会话拦截时记住目标路由，登录成功后原路返回，
 * 避免用户在抢课高峰期重新一路点回原页面。 */
let returnHash = "";

function showLogin() {
  try {
    const h = location.hash.replace(/^#\/?/, "");
    if (h && h !== "login" && !returnHash) returnHash = "#/" + h;
  } catch (e) { /* 忽略 */ }
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
    /* 登录成功 → 回到被拦截前的页面（A-07） */
    if (returnHash && location.hash !== returnHash) {
      const h = returnHash;
      returnHash = "";
      location.hash = h;
    }
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
  store.resetVolatile(); // I-09：退出登录即清空列表/心愿单/筛选，防下一账号看到上一账号数据
  document.getElementById("view").innerHTML = "";
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
    auth.setToken(null); store.setToken(null);
    store.resetVolatile(); // I-09：会话失效同样需要清空个人数据，避免残留展示
    document.getElementById("view").innerHTML = "";
    renderRoleBadge();
    showLogin();
    toast("登录已过期，请重新登录", "登录后将回到你刚才的页面", "warn", 2500);
  });

  // 登录页交互
  document.getElementById("loginRoles").addEventListener("click", (e) => {
    const c = e.target.closest(".chip");
    if (c) setLoginRole(c.dataset.role);
  });
  document.getElementById("loginBtn").onclick = doLogin;
  document.getElementById("loginPass").addEventListener("keydown", (e) => { if (e.key === "Enter") doLogin(); });
  document.getElementById("logoutBtn").onclick = doLogout;

  /* 账号下拉：点击空白处 / Esc / 路由切换时收起 */
  document.addEventListener("click", (e) => {
    if (!menuOpen) return;
    if (e.target.closest("#userMenu") || e.target.closest("#userChip")) return;
    closeUserMenu();
  });
  document.addEventListener("keydown", (e) => { if (e.key === "Escape") closeUserMenu(); });
  window.addEventListener("cp:switch-identity", (e) => switchIdentity(e.detail));
  store.on(() => syncThemeLabel(), "prefs");

  setLoginRole("STUDENT");
  renderRoleBadge();
  applyPrefs();        // 先用本地缓存的外观偏好着色，避免等待接口时的样式跳动
  syncThemeLabel();
  restorePendingLogin(); // 身份切换后回到登录页时回填账号
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

  document.getElementById("userChip").onclick = toggleUserMenu;

  document.getElementById("themeToggle").onclick = () => toggleTheme();

  bindPullRefresh(); // A-06：移动端下拉刷新（与页面内显式刷新按钮并存）

  bindSearch();
  window.addEventListener("hashchange", () => { closeUserMenu(); render(); });

  try {
    const [prefs, status] = await Promise.all([api.prefs(), api.status()]);
    store.setPrefs(prefs.data);
    syncThemeLabel();
    store.get().openAt = status.data.openAt;
  } catch (e) {
    applyPrefs();
    toast("无法连接后端", BACKEND_HINT, "danger", 4000);
  }

  await loadMe();
  await render();
}

/* ---------- 移动端下拉刷新（A-06） ----------
 * 仅在手机宽度、页面已在顶部、且无抽屉/弹窗打开时生效，
 * 与"抢课专区/课程中心"里的显式刷新按钮并存：一个手势、一个可点。 */
function bindPullRefresh() {
  const ind = document.getElementById("ptr");
  if (!ind) return;
  const mq = window.matchMedia("(max-width: 767px)");
  const THRESHOLD = 56;
  let startY = 0, dist = 0, pulling = false;

  const setInd = (d) => {
    if (d <= 0) { ind.style.transform = ""; ind.classList.remove("ready"); return; }
    ind.style.transform = `translateY(${Math.min(d, 64)}px)`;
    ind.classList.toggle("ready", d >= THRESHOLD);
    const t = ind.querySelector(".ptr-text");
    if (t) t.textContent = d >= THRESHOLD ? "松开刷新" : "下拉刷新";
  };

  const blocked = () => !mq.matches
    || document.body.classList.contains("drawer-lock")
    || !document.getElementById("loginScreen").hidden
    || !document.getElementById("modalRoot").hidden
    || document.getElementById("modalRoot").children.length > 0;

  window.addEventListener("touchstart", (e) => {
    if (blocked()) return;
    if (window.scrollY > 0 || document.documentElement.scrollTop > 0) return;
    startY = e.touches[0].clientY;
    dist = 0;
    pulling = true;
  }, { passive: true });

  window.addEventListener("touchmove", (e) => {
    if (!pulling) return;
    if (window.scrollY > 0 || document.documentElement.scrollTop > 0) { dist = 0; setInd(0); return; }
    const d = e.touches[0].clientY - startY;
    if (d <= 0) { dist = 0; setInd(0); return; }
    dist = d * 0.5; // 阻尼系数，避免下拉过于灵敏
    setInd(dist);
  }, { passive: true });

  window.addEventListener("touchend", () => {
    if (!pulling) return;
    pulling = false;
    if (dist >= THRESHOLD) {
      ind.classList.add("spin");
      setInd(64);
      const t = ind.querySelector(".ptr-text");
      if (t) t.textContent = "正在刷新…";
      setTimeout(() => {
        render();
        ind.classList.remove("spin");
        setInd(0);
        toast("已刷新", "", "ok", 1000);
      }, 420);
    } else setInd(0);
    dist = 0;
  });
}

boot();
