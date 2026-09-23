/* 前端 · 应用引导：导航、路由、全局事件 */

import { api } from "./api.js";
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
    view.innerHTML = `<div class="card"><h2>加载失败</h2><p style="color:var(--danger)">${String(e && e.message)}</p><p style="color:var(--tx-2);font-size:var(--fs-2)">请确认后端服务已启动（node server/index.js）。</p></div>`;
  }
  window.scrollTo(0, 0);
  refreshBadge();
}

/* ---------- 顶部倒计时 ---------- */
function startTopCountdown() {
  setInterval(() => {
    const node = document.getElementById("topCountdownText");
    const openAt = store.get().openAt;
    if (!node || !openAt) return;
    const diff = openAt - Date.now();
    if (diff <= 0) { node.textContent = "已开放"; return; }
    const s = Math.floor(diff / 1000);
    node.textContent = `${String(Math.floor(s / 60)).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`;
  }, 1000);
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

/* ---------- 引导 ---------- */
async function boot() {
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

  /* 学生 / 教师 角色切换 */
  document.getElementById("roleSwitch").addEventListener("click", (e) => {
    const btn = e.target.closest(".role-btn");
    if (!btn || btn.dataset.role === store.get().role) return;
    document.querySelectorAll(".role-btn").forEach((x) => x.classList.toggle("active", x === btn));
    store.setRole(btn.dataset.role);
    buildNav();
    const target = "#/" + homeFor(btn.dataset.role);
    if (location.hash !== target) location.hash = target; else render();
  });

  bindSearch();
  window.addEventListener("hashchange", render);

  try {
    const [prefs, status] = await Promise.all([api.prefs(), api.status()]);
    store.setPrefs(prefs.data);
    store.get().openAt = status.data.openAt;
  } catch (e) {
    applyPrefs();
    toast("无法连接后端", "请先运行 node server/index.js", "danger", 4000);
  }

  startTopCountdown();
  await loadMe();
  await render();
}

boot();
