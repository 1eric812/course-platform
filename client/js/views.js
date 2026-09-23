/* 前端 · 视图层：8 个页面的渲染与交互 */

import { api } from "./api.js";
import { store } from "./store.js";
import { icon, esc, el, toast, openModal, closeModal, confirmDialog, seatHtml, tagsHtml, scheduleText, pageHead, skeletonList, emptyBox } from "./ui.js";

let timers = [];
function tick(fn, ms) { const id = setInterval(fn, ms); timers.push(id); return id; }

/* 名额推送订阅：切页/重渲前先退订旧监听，避免 handler 叠加 */
let seatUnsub = null;
export function watchSeats(fn) {
  if (seatUnsub) seatUnsub();
  seatUnsub = api.onSeats(fn);
}
export function clearViewTimers() {
  timers.forEach(clearInterval); timers = [];
  if (seatUnsub) { seatUnsub(); seatUnsub = null; }
}

const DAYS = ["一", "二", "三", "四", "五", "六", "日"];
let filterCache = null;

function qsParams() {
  const f = store.get().filters;
  const p = new URLSearchParams();
  if (f.keyword) p.set("keyword", f.keyword);
  if (f.category.size) p.set("category", [...f.category].join(","));
  if (f.campus.size) p.set("campus", [...f.campus].join(","));
  if (f.assessment.size) p.set("assessment", [...f.assessment].join(","));
  if (f.credits.size) p.set("credits", [...f.credits].join(","));
  if (f.days.size) p.set("days", [...f.days].join(","));
  if (f.periods.size) p.set("periods", [...f.periods].join(","));
  if (f.teacher) p.set("teacher", f.teacher);
  if (f.onlyNoConflict) p.set("onlyNoConflict", "1");
  if (f.onlyAvailable) p.set("onlyAvailable", "1");
  if (f.sort && f.sort !== "default") p.set("sort", f.sort);
  const s = p.toString();
  return s ? s : "";
}

async function refreshCourses() {
  const { data } = await api.courses(qsParams());
  store.setCourses(data);
  updateCredit(data.credits, data.creditLimit);
  return data;
}

function updateCredit(used, limit) {
  const t = document.getElementById("creditText");
  const b = document.getElementById("creditBar");
  if (t) t.textContent = `${used} / ${limit}`;
  if (b) b.style.width = Math.min(100, Math.round((used / limit) * 100)) + "%";
}

/* ================= 首页工作台 ================= */
export async function renderDashboard(view) {
  view.innerHTML = pageHead("首页工作台", "待办、时间状态与快捷入口") + skeletonList(2);
  const [status, tt, wish, msgs] = await Promise.all([
    api.status(), api.timetable(), api.wishlist(), api.messages(),
  ]);
  store.get().openAt = status.data.openAt;
  /* P-01-1 选课节点：current=当前进行中，否则取最近一个未结束的阶段 */
  const phases = status.data.phases || [];
  const cur = phases.find((x) => x.current) || phases.find((x) => x.status !== "FINISHED") || null;
  const phaseLabel = cur ? `${cur.name}${cur.current ? "（进行中）" : "（待开始）"}` : "未设置时间节点";
  store.setWishlist(wish.data.items);
  store.setUnread(msgs.data.items.filter((m) => !m.read).length);
  const c = tt.data;
  const s = store.get();

  view.innerHTML = `
    ${pageHead("首页工作台", "待办、时间状态与快捷入口")}
    <div class="grid g4" style="margin-bottom:14px">
      <div class="stat"><div class="v">${phaseLabel}</div><div class="l">选课时间状态</div></div>
      <div class="stat"><div class="v">${c.courses.length}</div><div class="l">已选课程</div></div>
      <div class="stat"><div class="v">${c.totalCredits}</div><div class="l">已选学分（上限 ${s.courses.creditLimit || 30}）</div></div>
      <div class="stat"><div class="v">${wish.data.items.length}</div><div class="l">心愿单待提交</div></div>
    </div>

    ${phases.length ? `
    <div class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px">
        <h2 style="font-size:var(--fs-h2)">选课时间节点</h2>
        <span class="tag" style="color:${cur && cur.current ? "var(--ok)" : "var(--tx-3)"}">${esc(cur ? cur.name + (cur.current ? " 进行中" : " 待开始") : "待开放")}</span>
      </div>
      ${phases.map((x) => `
        <div class="tt-item${x.current ? " phase-current" : ""}">
          <span class="p">${fmtPeriod(x)}</span>
          <span><b>${esc(x.name)}</b>${x.current ? ' <span style="color:var(--ok)">进行中</span>' : x.status === "FINISHED" ? ' <span style="color:var(--tx-3)">已结束</span>' : ' <span style="color:var(--tx-3)">未开始</span>'}<br><span style="color:var(--tx-3)">${esc(x.remark || "按阶段说明执行")}</span></span>
        </div>`).join("")}
    </div>` : ""}

    <div class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px">
        <h2 style="font-size:var(--fs-h2)">快捷操作</h2>
      </div>
      <div class="grid g3">
        <button class="btn primary" data-nav="courses">${icon("search", 18)} 去选课</button>
        <button class="btn" data-nav="wishlist">${icon("heart", 18)} 心愿单</button>
        <button class="btn" data-nav="flash">${icon("bolt", 18)} 抢课专区</button>
      </div>
    </div>

    <div class="grid g2">
      <div class="card">
        <div style="display:flex;justify-content:space-between;margin-bottom:10px">
          <h2 style="font-size:var(--fs-h2)">我的课表（本周）</h2>
          <button class="btn sm" data-nav="timetable">查看全部</button>
        </div>
        ${c.courses.length ? c.courses.slice(0, 5).map((x) => `
          <div class="tt-item"><span class="p">${esc(scheduleText(x))}</span><span><b>${esc(x.name)}</b> · ${esc(x.place)}</span></div>
        `).join("") : emptyBox("本学期暂无已选课程", "前往课程中心添加")}
      </div>
      <div class="card">
        <div style="display:flex;justify-content:space-between;margin-bottom:10px">
          <h2 style="font-size:var(--fs-h2)">最新通知</h2>
          <button class="btn sm" data-nav="messages">全部消息</button>
        </div>
        ${msgs.data.items.slice(0, 4).map((m) => `
          <div class="tt-item"><span class="p">${esc(m.time)}</span><span><b>${esc(m.title)}</b><br><span style="color:var(--tx-3)">${esc(m.body)}</span></span></div>
        `).join("")}
      </div>
    </div>`;

  view.querySelectorAll("[data-nav]").forEach((b) => (b.onclick = () => location.hash = "#/" + b.dataset.nav));
}

/* P-01-1 选课节点：阶段起止时间的紧凑展示（MM-DD HH:mm） */
function fmtPeriod(p) {
  const s = String(p.start || "").slice(5, 16);
  const e = String(p.end || "").slice(5, 16);
  return s && e ? `${s} ~ ${e}` : "待定";
}

/* ================= 课程中心 ================= */
function errorBox(title, desc) {
  return `<div class="error-box">
    <div class="e-icon">${icon("warn", 26)}</div>
    <div class="e-title">${esc(title)}</div>
    <div class="e-desc">${esc(desc || "网络异常，请稍后重试")}</div>
    <button class="btn primary" data-retry>${icon("refresh", 18)} 一键重试</button>
  </div>`;
}

/* 当前生效的筛选条件（P-02-1 回显） */
function activeFilterTags() {
  const f = store.get().filters;
  const tags = [];
  const push = (label, clear) => tags.push({ label, clear });
  f.category.forEach((v) => push("类别:" + v, () => f.category.delete(v)));
  f.campus.forEach((v) => push("校区:" + v, () => f.campus.delete(v)));
  f.assessment.forEach((v) => push("考核:" + v, () => f.assessment.delete(v)));
  f.credits.forEach((v) => push(v + "学分", () => f.credits.delete(v)));
  f.days.forEach((v) => push("周" + DAYS[v - 1], () => f.days.delete(v)));
  f.periods.forEach((v) => push("第" + v + "节起", () => f.periods.delete(v)));
  if (f.teacher) push("教师:" + f.teacher, () => (f.teacher = ""));
  if (f.onlyNoConflict) push("仅看不冲突", () => (f.onlyNoConflict = false));
  if (f.onlyAvailable) push("仅看有余量", () => (f.onlyAvailable = false));
  return tags;
}

/* I-04 / P-05-2 名额实时更新：数值变化时高亮脉冲，并联动余量档位配色与进度条，
 * 让"名额变了"这件事被看见。课程中心与抢课专区共用同一实现，避免两处逻辑漂移。 */
function applySeatUpdate(payload) {
  if (!payload || payload.type !== "seats") return;
  const map = new Map(payload.seats.map((s) => [s.id, Number(s.remaining)]));
  document.querySelectorAll("[data-seat]").forEach((n) => {
    const id = Number(n.dataset.seat);
    if (!map.has(id)) return;
    const next = map.get(id);
    if (String(n.textContent) !== String(next)) {
      n.textContent = next;
      n.classList.add("pulse");
      setTimeout(() => n.classList.remove("pulse"), 1400);
    }
    const low = next > 0 && next <= 8;
    n.classList.toggle("low", low);
    n.classList.toggle("zero", next === 0);
    const bar = n.parentElement && n.parentElement.querySelector(".seat-bar i");
    const cap = Number(n.dataset.cap || 0);
    if (bar && cap > 0) {
      bar.style.width = Math.round((next / cap) * 100) + "%";
      bar.classList.toggle("low", low);
      bar.classList.toggle("zero", next === 0);
    }
  });
}

export async function renderCourses(view) {
  if (!filterCache) filterCache = (await api.filters()).data;
  const f = store.get().filters;

  view.innerHTML = `
    ${pageHead("课程中心", "多维筛选 · 四类冲突预检测 · 一键加入心愿单")}
    <div class="filters" id="filterPanel">
      <div class="f-head">
        <span class="f-head-title">${icon("filter", 18)} 筛选条件<span class="f-count" id="fCount" hidden></span></span>
        <div class="f-head-actions">
          <button class="btn sm" id="refreshCourses" type="button">${icon("refresh", 16)} 刷新名额</button>
          <button class="btn sm f-toggle" id="filterToggle" type="button" aria-expanded="true" aria-controls="filterRows">收起筛选</button>
          <button class="btn sm" id="clearFilters" type="button">清空筛选</button>
        </div>
      </div>
      <div class="f-rows" id="filterRows">
      <div class="f-row">
        <div class="f-group"><span class="f-label">课程类别</span><div class="chips" data-f="category">${filterCache.category.map((x) => `<button class="chip" data-v="${esc(x)}">${esc(x)}</button>`).join("")}</div></div>
        <div class="f-group"><span class="f-label">校区</span><div class="chips" data-f="campus">${filterCache.campus.map((x) => `<button class="chip" data-v="${esc(x)}">${esc(x)}</button>`).join("")}</div></div>
        <div class="f-group"><span class="f-label">考核方式</span><div class="chips" data-f="assessment">${filterCache.assessment.map((x) => `<button class="chip" data-v="${esc(x)}">${esc(x)}</button>`).join("")}</div></div>
        <div class="f-group"><span class="f-label">学分</span><div class="chips" data-f="credits">${filterCache.credits.map((x) => `<button class="chip" data-v="${x}">${x}</button>`).join("")}</div></div>
      </div>
      <div class="f-row" style="margin-top:12px">
        <div class="f-group"><span class="f-label">上课日</span><div class="chips" data-f="days">${filterCache.days.map((d) => `<button class="chip" data-v="${d}">周${DAYS[d - 1]}</button>`).join("")}</div></div>
        <div class="f-group"><span class="f-label">起始节次</span><div class="chips" data-f="periods">${filterCache.periods.map((p) => `<button class="chip" data-v="${p}">第${p}节</button>`).join("")}</div></div>
        <div class="f-group"><span class="f-label">授课教师</span>
          <input class="inp" id="fTeacher" list="teacherList" placeholder="输入教师姓名" style="width:150px" value="${esc(f.teacher || "")}">
          <datalist id="teacherList">${filterCache.teachers.map((t) => `<option value="${esc(t)}">`).join("")}</datalist>
        </div>
      </div>
      <div class="f-row" style="margin-top:12px;align-items:center">
        <label class="switch"><input type="checkbox" id="swConflict"> 仅看不冲突</label>
        <label class="switch"><input type="checkbox" id="swAvail"> 仅看有余量</label>
        <select class="inp" id="sortSel">
          <option value="default">默认排序</option>
          <option value="remaining">按剩余名额</option>
          <option value="rating">按评分</option>
          <option value="credits">按学分</option>
        </select>
      </div>
      <div class="f-done"><button class="btn primary" id="filterDone" type="button">完成并查看结果</button></div>
      </div><!-- /f-rows 移动端折叠区 -->
      <div class="ftag-row" id="ftagRow"></div>
      <div class="f-summary" id="fSummary"></div>
    </div>
    <div class="f-scrim" id="fScrim" hidden></div>
    <div class="course-list" id="courseList">${skeletonList(6)}</div>`;

  syncFilterUI();
  renderFtags();
  bindFilterPanel();
  updateFilterChrome();

  const reload = async () => {
    const listEl = document.getElementById("courseList");
    if (listEl) listEl.innerHTML = skeletonList(6);
    try {
      await refreshCourses();
      renderCourseList();
    } catch (e) {
      if (listEl) {
        listEl.innerHTML = errorBox("课程加载失败", (e && e.message) || "接口异常，可能是网络波动或服务未启动");
        const rb = listEl.querySelector("[data-retry]");
        if (rb) rb.onclick = reload;
      }
    }
  };

  view.querySelectorAll(".chips").forEach((group) => {
    group.addEventListener("click", (e) => {
      const btn = e.target.closest(".chip");
      if (!btn) return;
      const key = group.dataset.f, val = key === "days" || key === "periods" ? Number(btn.dataset.v) : btn.dataset.v;
      const set = store.get().filters[key];
      set.has(val) ? set.delete(val) : set.add(val);
      btn.classList.toggle("on");
      renderFtags(); reload();
    });
  });
  let teacherT = null;
  view.querySelector("#fTeacher").addEventListener("input", (e) => {
    clearTimeout(teacherT);
    teacherT = setTimeout(() => { store.get().filters.teacher = e.target.value.trim(); renderFtags(); reload(); }, 350);
  });
  view.querySelector("#swConflict").onchange = (e) => { store.get().filters.onlyNoConflict = e.target.checked; renderFtags(); reload(); };
  view.querySelector("#swAvail").onchange = (e) => { store.get().filters.onlyAvailable = e.target.checked; renderFtags(); reload(); };
  view.querySelector("#sortSel").onchange = (e) => { store.get().filters.sort = e.target.value; reload(); };
  view.querySelector("#clearFilters").onclick = () => {
    const fl = store.get().filters;
    fl.category.clear(); fl.campus.clear(); fl.assessment.clear(); fl.credits.clear(); fl.days.clear(); fl.periods.clear();
    fl.teacher = ""; fl.onlyNoConflict = false; fl.onlyAvailable = false; fl.sort = "default";
    const ti = document.getElementById("fTeacher"); if (ti) ti.value = "";
    syncFilterUI(); renderFtags(); reload();
  };

  await reload();

  /* 显著的刷新控件（I-04）：随时可手动核对名额，不必依赖自动推送 */
  const rf = view.querySelector("#refreshCourses");
  if (rf) rf.onclick = async () => { await reload(); toast("名额已刷新", "已按最新余量重排", "ok", 1200); };

  // 实时名额更新（SSE，断线自动降级轮询）
  watchSeats(applySeatUpdate);
}

function renderFtags() {
  const row = document.getElementById("ftagRow");
  if (!row) return;
  const tags = activeFilterTags();
  row.innerHTML = tags.length
    ? `<span style="font-size:var(--fs-2);color:var(--tx-3)">已选条件：</span>` +
      tags.map((t, i) => `<span class="ftag">${esc(t.label)}<button data-ftag="${i}" aria-label="删除">×</button></span>`).join("") +
      `<button class="ftag-clear" data-ftag-all>清除全部</button>`
    : "";
  row.querySelectorAll("[data-ftag]").forEach((b) => (b.onclick = () => {
    const tags2 = activeFilterTags();
    const t = tags2[Number(b.dataset.ftag)];
    if (t) t.clear();
    syncFilterUI(); renderFtags();
    const listEl = document.getElementById("courseList");
    refreshCourses().then(renderCourseList);
  }));
  const all = row.querySelector("[data-ftag-all]");
  if (all) all.onclick = () => document.getElementById("clearFilters").click();
  updateFilterChrome();
}

/* 移动端筛选区（P-01-6 / A-03 / A-05）：
 * - 小屏默认收起，仅留"筛选条件"摘要条，避免筛选区把课程列表挤出首屏；
 * - 展开时升级为底部抽屉：只占约 2/3 屏高、可滚动、带遮罩与"完成"按钮，
 *   保证筛选、查看结果在拇指可及的范围内完成，且不会误触背景。 */
function bindFilterPanel() {
  const panel = document.getElementById("filterPanel");
  const toggle = document.getElementById("filterToggle");
  const rows = document.getElementById("filterRows");
  const scrim = document.getElementById("fScrim");
  const done = document.getElementById("filterDone");
  if (!panel || !toggle || !rows) return;
  const mq = window.matchMedia("(max-width: 900px)");
  let open = !mq.matches;

  const apply = () => {
    const mobile = mq.matches;
    if (!mobile) open = true;
    toggle.hidden = !mobile;
    rows.hidden = !open;
    const drawer = mobile && open;
    panel.classList.toggle("collapsed", !open);
    panel.classList.toggle("drawer", drawer);
    document.body.classList.toggle("drawer-lock", drawer);
    if (scrim) scrim.hidden = !drawer;
    toggle.setAttribute("aria-expanded", String(open));
    toggle.textContent = open ? "收起筛选" : "展开筛选";
    updateFilterChrome();
  };

  apply();
  toggle.onclick = () => { open = !open; apply(); };
  if (done) done.onclick = () => { open = false; apply(); };   // 一次点击回到结果列表
  if (scrim) scrim.onclick = () => { open = false; apply(); };
  const onMq = () => { open = !mq.matches; apply(); };
  if (mq.addEventListener) mq.addEventListener("change", onMq); else mq.addListener(onMq);
}

/* 已选条件计数：折叠状态下也能看到"筛选条件 · 已选 N"，提示存在生效中的筛选（避免"看不到结果却不知为何"） */
function updateFilterChrome() {
  const n = activeFilterTags().length;
  const badge = document.getElementById("fCount");
  if (badge) {
    badge.textContent = n ? `已选 ${n}` : "";
    badge.hidden = n === 0;
  }
  const toggle = document.getElementById("filterToggle");
  if (toggle && !toggle.hidden && toggle.getAttribute("aria-expanded") === "false") {
    toggle.textContent = n ? `展开筛选（${n}）` : "展开筛选";
  }
}

function syncFilterUI() {
  const f = store.get().filters;
  document.querySelectorAll(".chips").forEach((g) => {
    g.querySelectorAll(".chip").forEach((b) => {
      const val = g.dataset.f === "days" || g.dataset.f === "periods" ? Number(b.dataset.v) : b.dataset.v;
      b.classList.toggle("on", f[g.dataset.f].has(val));
    });
  });
  const sc = document.getElementById("swConflict"); if (sc) sc.checked = f.onlyNoConflict;
  const sa = document.getElementById("swAvail"); if (sa) sa.checked = f.onlyAvailable;
  const so = document.getElementById("sortSel"); if (so) so.value = f.sort;
}

function renderCourseList() {
  const listEl = document.getElementById("courseList");
  if (!listEl) return;
  const { items, total } = store.get().courses;
  const fSummary = document.getElementById("fSummary");
  if (fSummary) fSummary.innerHTML = `<span>共 ${total} 门课程</span><span>已选 ${store.get().courses.credits} 学分</span>`;
  if (!items.length) {
    listEl.innerHTML = emptyBox("没有符合条件的课程", "试试调整筛选条件") + `<div style="text-align:center"><button class="btn sm" id="emptyClear">清除筛选条件</button></div>`;
    const ec = listEl.querySelector("#emptyClear");
    if (ec) ec.onclick = () => { const cb = document.getElementById("clearFilters"); if (cb) cb.click(); };
    return;
  }
  listEl.innerHTML = items.map((c) => `
    <div class="course-card ${c.conflict ? "conflict" : ""}">
      <div class="cc-main">
        <div class="cc-title"><span class="cc-name">${esc(c.name)}</span><span class="cc-code">${esc(c.code)}</span>${tagsHtml(c)}</div>
        <div class="cc-meta">
          <span><b>教师</b> ${esc(c.teacher)}</span>
          <span><b>学分</b> ${c.credits}</span>
          <span><b>时间</b> ${esc(scheduleText(c))}</span>
          <span><b>地点</b> ${esc(c.campus)} ${esc(c.place)}</span>
          <span><b>考核</b> ${esc(c.assessment)}</span>
          <span><b>评分</b> ${c.rating}</span>
          ${c.prereq ? `<span><b>先修</b> ${esc(c.prereq)}</span>` : ""}
        </div>
        ${(c.conflictDetail || []).map((x) => `<div class="cc-why" style="color:${x.level === "hard" ? "var(--danger-t)" : "var(--warn-t)"}">${icon("info", 14)} ${esc(x.message)}</div>`).join("")}
      </div>
      <div class="cc-side">
        ${seatHtml(c)}
        ${c.selected
          ? `<button class="btn sm" disabled>${icon("check", 16)} 已选</button>`
          : c.inWishlist
          ? `<button class="btn sm" data-nav-wish>${icon("heart", 16)} 心愿单</button>`
          : c.remaining === 0
          ? `<button class="btn sm ${c.subscribed ? "subscribed" : ""}" data-sub="${c.id}">${icon("bell", 16)} ${c.subscribed ? "已订阅" : "订阅放号"}</button>`
          : `<button class="btn sm primary" data-add="${c.id}">${icon("plus", 16)} 加入心愿单</button>`}
      </div>
    </div>`).join("");
  listEl.querySelectorAll("[data-nav-wish]").forEach((b) => (b.onclick = () => (location.hash = "#/wishlist")));
  listEl.querySelectorAll("[data-add]").forEach((b) => (b.onclick = async () => {
    const r = await api.wishlistAdd(Number(b.dataset.add));
    if (r.data.error) toast("无法加入", r.data.error, "warn");
    else { toast("已加入心愿单", "可在心愿单中调整志愿顺序", "ok"); refreshCourses().then(renderCourseList); refreshBadge(); }
  }));
  listEl.querySelectorAll("[data-sub]").forEach((b) => (b.onclick = async () => {
    const r = await api.subscribe(Number(b.dataset.sub));
    if (r.data.error) toast("无法订阅", r.data.error, "warn");
    else { toast("已订阅放号通知", "名额恢复时会通过消息提醒你", "ok"); refreshCourses().then(renderCourseList); }
  }));
}

/* ================= 心愿单 ================= */
export async function renderWishlist(view) {
  view.innerHTML = pageHead("心愿单", "志愿梯队 · 开放瞬间一键批量提交") + skeletonList(2);
  const { data } = await api.wishlist();
  store.setWishlist(data.items);
  const items = data.items;

  view.innerHTML = `
    ${pageHead("心愿单", "志愿梯队 · 开放瞬间一键批量提交")}
    ${items.length ? `
      <div class="card" style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px">
        <div>
          <div style="font-weight:600">共 ${items.length} 门待提交</div>
          <div style="font-size:var(--fs-2);color:var(--tx-2)">按志愿顺序依次尝试，首选失败自动降级</div>
        </div>
        <button class="btn primary wl-submit-desktop" id="submitAll">${icon("send", 18)} 一键提交选课</button>
      </div>
      <div id="wlList"></div>
      <div class="action-bar"><button class="btn primary" id="submitAllM" style="flex:1">${icon("send", 18)} 一键提交选课（${items.length} 门）</button></div>
    ` : emptyBox("心愿单为空", "前往课程中心添加心仪课程") + `<div style="text-align:center"><button class="btn primary" data-nav="courses">${icon("plus", 18)} 去添加课程</button></div>`}
  `;

  const navBtn = view.querySelector("[data-nav]");
  if (navBtn) navBtn.onclick = () => (location.hash = "#/courses");
  const listEl = view.querySelector("#wlList");
  if (listEl && items.length) renderWlList(listEl, items);

  const doSubmit = async (btn) => {
    const ok = await confirmDialog("确认批量提交", `将按志愿顺序提交 ${items.length} 门课程，提交后立即返回受理回执，结果稍后推送。`);
    if (!ok) return;
    btn.disabled = true;
    const r = await api.submit();
    if (r.data.error) { toast("提交失败", r.data.error, "danger"); btn.disabled = false; return; }
    toast("已受理", `受理编号 ${r.data.ticket.id} · 排队第 ${r.data.ticket.queuePos} 位`, "ok");
    pollTicket(r.data.ticket.id, r.data.ticket);
    btn.disabled = false;
  };
  const submitBtn = view.querySelector("#submitAll");
  if (submitBtn) submitBtn.onclick = () => doSubmit(submitBtn);
  const submitBtnM = view.querySelector("#submitAllM");
  if (submitBtnM) submitBtnM.onclick = () => doSubmit(submitBtnM);
}

function renderWlList(listEl, items) {
  listEl.innerHTML = items.map((w) => {
    const c = w.course;
    return `
    <div class="wl-item ${w.blocked ? "conflict" : ""}">
      <div class="wl-rank">${w.priority}</div>
      <div class="wl-body">
        <div class="wl-name">${esc(c.name)} <span class="cc-code">${esc(c.code)}</span></div>
        <div class="wl-sub">${esc(scheduleText(c))} · ${esc(c.teacher)} · ${c.credits} 学分 · 余 ${c.remaining}</div>
        ${w.conflict.length ? `<div class="wl-warn">${icon("info", 14)} ${esc(w.conflict[0].message)}</div>` : ""}
      </div>
      <div style="display:flex;gap:6px">
        <button class="btn sm" data-up="${w.courseId}" ${w.priority === 1 ? "disabled" : ""} aria-label="上移">${icon("up", 16)}</button>
        <button class="btn sm" data-down="${w.courseId}" ${w.priority === items.length ? "disabled" : ""} aria-label="下移">${icon("down", 16)}</button>
        <button class="btn sm danger" data-del="${w.courseId}" aria-label="移除">${icon("trash", 16)}</button>
      </div>
    </div>`;
  }).join("");

  listEl.querySelectorAll("[data-up]").forEach((b) => (b.onclick = () => moveWish(b.dataset.up, -1)));
  listEl.querySelectorAll("[data-down]").forEach((b) => (b.onclick = () => moveWish(b.dataset.down, 1)));
  listEl.querySelectorAll("[data-del]").forEach((b) => (b.onclick = async () => {
    await api.wishlistRemove(Number(b.dataset.del));
    toast("已移除", "", "ok");
    renderWishlist(document.getElementById("view"));
    refreshBadge();
  }));
}

async function moveWish(courseId, dir) {
  const items = store.get().wishlist.map((w) => w.courseId);
  const idx = items.indexOf(Number(courseId));
  if (idx < 0) return;
  const j = idx + dir;
  if (j < 0 || j >= items.length) return;
  [items[idx], items[j]] = [items[j], items[idx]];
  await api.wishlistReorder(items);
  renderWishlist(document.getElementById("view"));
}

function pollTicket(ticketId, firstTicket) {
  const node = openModal({ title: "选课受理", body: `<div id="ticketLive"></div>`, actions: [] });

  const render = (t) => {
    const body = node.querySelector("#ticketLive");
    if (!body || !t) return;
    const done = t.status === "成功" || t.status === "失败";
    const curIdx = t.status === "已受理" ? 0 : t.status === "处理中" ? 1 : 2;
    const steps = ["已受理", "处理中", done ? t.status : "成功/失败"];
    const cls = (i) => (i < curIdx ? "done" : i === curIdx ? (done ? (t.status === "失败" ? "fail" : "done") : "cur") : "");
    body.innerHTML = `
      <div class="t-steps">${steps.map((s, i) => `<div class="t-step ${cls(i)}">${s}</div>`).join("")}</div>
      <div style="text-align:center;color:var(--tx-2);font-size:var(--fs-2)">
        受理编号 <span class="mono">${esc(t.id)}</span>${t.queuePos ? ` · 排队第 ${t.queuePos} 位` : ""}
      </div>
      ${done ? `
        <div class="grid g2" style="margin:14px 0 10px">
          <div class="stat"><div class="v" style="color:var(--ok)">${t.accepted.length}</div><div class="l">成功</div></div>
          <div class="stat"><div class="v" style="color:var(--danger)">${t.rejected.length}</div><div class="l">失败</div></div>
        </div>
        ${t.accepted.length ? `<div class="card" style="padding:10px"><b style="color:var(--ok)">成功</b><div style="font-size:var(--fs-2);margin-top:4px">${t.accepted.map((a) => esc(a.name)).join("、")}</div></div>` : ""}
        ${t.rejected.length ? `<div class="card" style="padding:10px"><b style="color:var(--danger)">失败原因</b><div style="font-size:var(--fs-2);margin-top:4px">${t.rejected.map((a) => `${esc(a.name)}：${esc(a.reason)}`).join("<br>")}</div></div>` : ""}
        <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:8px">
          <button class="btn" id="ticketClose">知道了</button>
          <button class="btn primary" id="ticketTt">查看课表</button>
        </div>`
      : `<div style="text-align:center;padding:16px;color:var(--tx-3)">正在为你处理，请稍候…</div>`}`;
    if (done) {
      const cl = body.querySelector("#ticketClose"); if (cl) cl.onclick = () => closeModal();
      const tt = body.querySelector("#ticketTt"); if (tt) tt.onclick = () => { closeModal(); location.hash = "#/timetable"; };
    }
  };

  render(firstTicket);
  /* 用 tick() 纳入统一清理：切页时 clearViewTimers() 兜底，避免跨页泄漏 */
  const id = tick(async () => {
    const r = await api.ticket(ticketId);
    const t = r.data;
    if (!t || t.error) { clearInterval(id); timers = timers.filter((x) => x !== id); return; }
    render(t);
    if (t.status === "成功" || t.status === "失败") {
      clearInterval(id);
      timers = timers.filter((x) => x !== id);
      renderWishlist(document.getElementById("view"));
      refreshBadge();
    }
  }, 500);
}

/* ================= 抢课专区 ================= */
export async function renderFlash(view) {
  view.innerHTML = pageHead("抢课专区", "时间状态 · 实时名额 · 目标监控 · 异常可重试") + skeletonList(2);
  let currentPhase = null;
  let selectionOpen = true;
  try {
    const status = await api.status();
    const phases = status.data.phases || [];
    currentPhase = phases.find((x) => x.current) || phases.find((x) => x.status !== "FINISHED") || null;
    /* 教务手动关闸与「不在阶段窗口内」是两个独立条件，这里读后端下发的开关 */
    selectionOpen = status.data.selectionOpen !== false;
    store.get().openAt = status.data.openAt;
  } catch (e) { /* 选课状态接口可容忍失败 */ }

  const phaseText = currentPhase ? `${currentPhase.name}${currentPhase.current ? "（进行中）" : "（待开始）"}` : "未设置选课活动";
  const phaseSpan = currentPhase ? `${String(currentPhase.start || "").slice(5, 16)} ~ ${String(currentPhase.end || "").slice(5, 16)}` : "待配置时间节点";

  view.innerHTML = `
    ${pageHead("抢课专区", "时间状态 · 实时名额 · 目标监控 · 异常可重试")}
    <div class="card" style="text-align:center;padding:22px">
      <div style="font-size:var(--fs-2);color:var(--tx-2)">当前选课阶段</div>
      <div style="font-size:36px;font-weight:600;line-height:1.2;margin:8px 0;color:var(--pri)">${esc(phaseText)}</div>
      <div style="font-size:var(--fs-2);color:var(--tx-3)">${esc(phaseSpan)}</div>
      <div id="flashCd" class="cd-line" aria-live="polite"></div>
      ${selectionOpen ? "" : `<div class="flash-closed">${icon("warn", 16)} 教务已关闭选课通道，暂不受理提交</div>`}
      <div style="margin-top:12px"><button class="btn" id="flashRefresh">${icon("refresh", 18)} 手动刷新名额</button></div>
    </div>
    <div class="card">
      <h2 style="font-size:var(--fs-h2);margin-bottom:10px">名额监控（实时）</h2>
      <div id="flashList">${skeletonList(3)}</div>
    </div>
    <button class="fab-refresh" id="flashFab" type="button" aria-label="刷新名额">${icon("refresh", 20)}<span>刷新</span></button>`;

  const listEl = view.querySelector("#flashList");
  const renderList = (items) => {
    if (!listEl) return;
    listEl.innerHTML = items.length ? items.map((c) => `
      <div class="wl-item">
        <div class="wl-body">
          <div class="wl-name">${esc(c.name)} <span class="cc-code">${esc(c.code)}</span></div>
          <div class="wl-sub">${esc(scheduleText(c))} · ${esc(c.teacher)}</div>
        </div>
        <div class="seat"><div class="n ${c.remaining === 0 ? "zero" : c.remaining <= 8 ? "low" : ""}" data-seat="${c.id}">${c.remaining}</div><div class="c">剩余</div></div>
        ${c.remaining === 0
          ? `<button class="btn sm ${c.subscribed ? "subscribed" : ""}" data-sub="${c.id}">${icon("bell", 16)} ${c.subscribed ? "已订阅" : "订阅放号"}</button>`
          : `<button class="btn sm primary" data-add="${c.id}">${icon("plus", 16)} 抢</button>`}
      </div>`).join("") : emptyBox("暂无可监控课程");
    bindFlashActions();
  };
  const bindFlashActions = () => {
    listEl.querySelectorAll("[data-add]").forEach((b) => (b.onclick = async () => {
      const r = await api.wishlistAdd(Number(b.dataset.add));
      if (r.data.error) toast("无法加入", r.data.error, "warn");
      else { toast("已加入心愿单", "", "ok"); refreshBadge(); load(); }
    }));
    listEl.querySelectorAll("[data-sub]").forEach((b) => (b.onclick = async () => {
      const r = await api.subscribe(Number(b.dataset.sub));
      if (r.data.error) toast("无法订阅", r.data.error, "warn");
      else { toast("已订阅放号通知", "名额恢复时会通过消息提醒你", "ok"); load(); }
    }));
  };

  const load = async () => {
    if (listEl) listEl.innerHTML = skeletonList(3);
    try {
      const r = await api.courses("sort=remaining");
      renderList(r.data.items.slice(0, 10));
    } catch (e) {
      if (listEl) {
        listEl.innerHTML = errorBox("名额监控加载失败", (e && e.message) || "接口异常");
        const rb = listEl.querySelector("[data-retry]");
        if (rb) rb.onclick = load;
      }
    }
  };
  await load();

  /* P-05-1 倒计时与最后 60 秒强调：开放瞬间是抢课峰值，提前给出明确的心理预期与行动信号 */
  const cdEl = view.querySelector("#flashCd");
  if (cdEl) {
    const upd = () => {
      const openAt = Number(store.get().openAt || 0);
      const diff = openAt - Date.now();
      if (openAt && diff > 0) {
        const d = Math.floor(diff / 86400000);
        const h = Math.floor((diff % 86400000) / 3600000);
        const m = Math.floor((diff % 3600000) / 60000);
        const s = Math.floor((diff % 60000) / 1000);
        const urgent = diff <= 60000;
        cdEl.classList.toggle("urgent", urgent);
        cdEl.innerHTML = urgent
          ? `距开放不足 1 分钟，请做好准备！<br><b>${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}</b>`
          : `距开放还有 <b>${d ? d + " 天 " : ""}${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}</b>`;
      } else if (openAt && diff > -600000) {
        cdEl.classList.remove("urgent");
        cdEl.textContent = "已开放 · 名额实时刷新中";
      } else {
        cdEl.classList.remove("urgent");
        cdEl.textContent = currentPhase && currentPhase.current ? "选课进行中" : "暂未配置开放时间";
      }
    };
    upd();
    tick(upd, 1000);
  }

  const onRefresh = async () => { await load(); toast("已刷新", "名额已按最新余量更新", "ok", 1200); };
  view.querySelector("#flashRefresh").onclick = onRefresh;
  const fab = view.querySelector("#flashFab");
  if (fab) fab.onclick = onRefresh;

  watchSeats(applySeatUpdate);
}

/* ================= 我的课表 ================= */
export async function renderTimetable(view) {
  view.innerHTML = pageHead("我的课表", "周视图 / 列表视图 / 按日视图 · 支持导出") + skeletonList(2);
  const { data } = await api.timetable();
  const s = store.get();
  const isMobile = window.matchMedia("(max-width: 767px)").matches;
  let mode = isMobile ? "day" : (s.prefs.timetableView || "week");

  view.innerHTML = `
    ${pageHead("我的课表", "周视图 / 列表视图 / 按日视图 · 支持导出")}
    <div class="card" style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px">
      <div>本学期共 <b>${data.courses.length}</b> 门 · <b>${data.totalCredits}</b> 学分</div>
      <div style="display:flex;gap:8px;flex-wrap:wrap;align-items:center">
        <div class="chips" id="ttViewChips">
          ${isMobile ? `<button class="chip ${mode === "day" ? "on" : ""}" data-v="day">按日</button>` : ""}
          <button class="chip ${mode === "week" ? "on" : ""}" data-v="week">周视图</button>
          <button class="chip ${mode === "list" ? "on" : ""}" data-v="list">列表视图</button>
        </div>
        <button class="btn sm" id="exportIcs">${icon("down", 16)} 导出日历</button>
      </div>
    </div>
    <div id="ttBody"></div>`;

  const body = view.querySelector("#ttBody");
  let activeDay = (() => { const d = new Date().getDay(); return d === 0 ? 7 : d; })();

  const render = () => {
    view.querySelectorAll("#ttViewChips .chip").forEach((x) => x.classList.toggle("on", x.dataset.v === mode));
    if (mode === "week") body.innerHTML = weekGrid(data.courses);
    else if (mode === "list") body.innerHTML = listView(data.courses);
    else {
      body.innerHTML = dayFlow(data.courses, activeDay);
      body.querySelectorAll("[data-day]").forEach((b) => (b.onclick = () => { activeDay = Number(b.dataset.day); render(); }));
      const step = (delta) => { activeDay = ((activeDay + delta - 1 + 7) % 7) + 1; render(); };
      const l = body.querySelector("#ttDayPrev"), r = body.querySelector("#ttDayNext");
      if (l) l.onclick = () => step(-1);
      if (r) r.onclick = () => step(1);
      /* A-06 移动端手势：左右滑动切换日期，避免每次都要点按 */
      let sx = null, sy = null;
      body.addEventListener("touchstart", (e) => { sx = e.touches[0].clientX; sy = e.touches[0].clientY; }, { passive: true });
      body.addEventListener("touchend", (e) => {
        if (sx === null) return;
        const t = e.changedTouches[0];
        const dx = t.clientX - sx, dy = t.clientY - sy;
        sx = null; sy = null;
        if (Math.abs(dx) > 48 && Math.abs(dx) > Math.abs(dy)) step(dx < 0 ? 1 : -1);
      }, { passive: true });
    }
  };
  render();

  view.querySelectorAll("#ttViewChips .chip").forEach((b) => (b.onclick = async () => {
    mode = b.dataset.v;
    render();
    if (mode !== "day") { store.setPrefs({ timetableView: mode }); await api.setPrefs({ timetableView: mode }); }
  }));

  view.querySelector("#exportIcs").onclick = () => {
    api.exportIcs();
    toast("已生成课表日历文件", "下载后可用系统日历导入", "ok", 1800);
  };
}

function dayCount(courses, day) {
  let n = 0;
  courses.forEach((c) => c.schedule.forEach((s) => { if (s.day === day) n++; }));
  return n;
}

function dayFlow(courses, day) {
  const items = [];
  courses.forEach((c) => c.schedule.forEach((sch) => { if (sch.day === day) items.push({ c, sch }); }));
  items.sort((a, b) => a.sch.start - b.sch.start);
  return `
    <div class="day-nav">
      <button class="btn sm" id="ttDayPrev" type="button" aria-label="前一天">‹ 前一天</button>
      <div class="day-tabs">${[1, 2, 3, 4, 5, 6, 7].map((d) => {
        const n = dayCount(courses, d);
        return `<button class="day-tab ${d === day ? "on" : ""}" data-day="${d}"><span>周${DAYS[d - 1]}</span><span class="d">${n ? n + " 节" : "休息"}</span></button>`;
      }).join("")}</div>
      <button class="btn sm" id="ttDayNext" type="button" aria-label="后一天">后一天 ›</button>
    </div>
    ${items.length ? `<div class="tt-day">${items.map(({ c, sch }) => `
        <div class="tt-item"><span class="p">第 ${sch.start}-${sch.end} 节</span><span><b>${esc(c.name)}</b> · ${esc(c.teacher)} · ${esc(c.place)} · ${esc(c.campus)}</span></div>`).join("")}</div>`
      : emptyBox(`周${DAYS[day - 1]}没有课，好好休息`)}`;
}

function weekGrid(courses) {
  const map = {};
  courses.forEach((c) => c.schedule.forEach((sch) => {
    for (let p = sch.start; p <= sch.end; p++) map[`${sch.day}-${p}`] = c;
  }));
  const periods = 12;
  let html = `<div class="tt-wrap"><table class="tt"><thead><tr><th>节次</th>${DAYS.map((d) => `<th>周${d}</th>`).join("")}</tr></thead><tbody>`;
  for (let p = 1; p <= periods; p++) {
    html += `<tr><td class="period">${p}</td>`;
    for (let d = 1; d <= 7; d++) {
      const c = map[`${d}-${p}`];
      const isStart = c && c.schedule.some((s) => s.day === d && s.start === p);
      html += isStart
        ? `<td rowspan="${span(c, d, p)}"><div class="tt-slot" style="background:${slotColor(c.id)};color:#fff"><span class="sn">${esc(c.name)}</span><span class="sp">${esc(c.place)}</span></div></td>`
        : c ? "" : `<td></td>`;
    }
    html += "</tr>";
  }
  return html + "</tbody></table></div>";
}
function span(c, d, p) { const s = c.schedule.find((x) => x.day === d && x.start === p); return s ? s.end - s.start + 1 : 1; }
function slotColor(id) { const colors = ["#1D4ED8", "#0891B2", "#059669", "#D97706", "#7C3AED", "#DC2626", "#0D9488"]; return colors[id % colors.length]; }

function listView(courses) {
  if (!courses.length) return emptyBox("本学期暂无已选课程");
  const byDay = {};
  courses.forEach((c) => c.schedule.forEach((sch) => {
    (byDay[sch.day] = byDay[sch.day] || []).push({ c, sch });
  }));
  const days = Object.keys(byDay).map(Number).sort();
  return `<div class="tt-list">${days.map((d) => `
    <div class="tt-day"><h3>周${DAYS[d - 1]}</h3>
      ${byDay[d].sort((a, b) => a.sch.start - b.sch.start).map(({ c, sch }) => `
        <div class="tt-item"><span class="p">第 ${sch.start}-${sch.end} 节</span><span><b>${esc(c.name)}</b> · ${esc(c.teacher)} · ${esc(c.place)} · ${esc(c.campus)}</span></div>
      `).join("")}
    </div>`).join("")}</div>`;
}

/* ================= 消息中心 ================= */
export async function renderMessages(view) {
  view.innerHTML = pageHead("消息中心", "选课结果 · 名额变化 · 时间节点 · 系统公告") + skeletonList(2);
  const { data } = await api.messages();
  store.setUnread(data.items.filter((m) => !m.read).length);
  const notify = store.get().prefs.notify || {};
  const typeName = { system: "系统公告", result: "选课结果", seat: "名额变化", remind: "时间提醒", drop: "退课提醒" };
  const jumpFor = { result: ["timetable", "查看课表"], seat: ["flash", "去抢课"], remind: ["timetable", "查看课表"] };
  const items = data.items.filter((m) => notify[m.type] !== false);

  view.innerHTML = `
    ${pageHead("消息中心", "选课结果 · 名额变化 · 时间节点 · 系统公告")}
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px">
        <h2 style="font-size:var(--fs-h2)">消息订阅</h2>
        <span style="font-size:var(--fs-2);color:var(--tx-3)">关闭后不再展示该类型</span>
      </div>
      <div class="chips" id="notifyChips">
        ${Object.keys(typeName).map((t) => `<button class="chip ${notify[t] !== false ? "on" : ""}" data-t="${t}">${typeName[t]}</button>`).join("")}
      </div>
    </div>
    <div class="card" style="display:flex;justify-content:space-between;align-items:center">
      <div>共 ${items.length} 条 · 未读 ${items.filter((m) => !m.read).length} 条</div>
      <button class="btn sm" id="readAll">${icon("check", 16)} 全部标记已读</button>
    </div>
    <div class="card" style="padding:0">
      ${items.length ? items.map((m) => `
        <div class="msg ${m.read ? "read" : ""}">
          <span class="msg-dot"></span>
          <div style="flex:1;min-width:0">
            <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
              <span class="msg-title">${esc(m.title)}</span>
              <span class="tag">${esc(typeName[m.type] || m.type)}</span>
              ${jumpFor[m.type] ? `<button class="btn sm" style="margin-left:auto;min-height:28px" data-jump="${jumpFor[m.type][0]}">${jumpFor[m.type][1]}</button>` : ""}
            </div>
            <div class="msg-body">${esc(m.body)}</div>
            <div class="msg-time">${esc(m.time)}</div>
          </div>
        </div>`).join("") : emptyBox("暂无消息（可能已关闭全部订阅类型）")}
    </div>`;

  view.querySelector("#notifyChips").addEventListener("click", async (e) => {
    const b = e.target.closest(".chip"); if (!b) return;
    const t = b.dataset.t;
    const n = { ...(store.get().prefs.notify || {}) };
    n[t] = !(n[t] !== false);
    store.setPrefs({ notify: n });
    await api.setPrefs({ notify: n });
    renderMessages(view);
  });
  view.querySelectorAll("[data-jump]").forEach((b) => (b.onclick = () => (location.hash = "#/" + b.dataset.jump)));
  view.querySelector("#readAll").onclick = async () => {
    await api.readMessages();
    toast("已全部标记为已读", "", "ok");
    renderMessages(view);
    refreshBadge();
  };
}

/* ================= 个人中心 ================= */
export async function renderSettings(view) {
  view.innerHTML = pageHead("个人中心", "偏好设置与个性化") + skeletonList(1);
  const [{ data: prefs }, { data: me }] = await Promise.all([api.prefs(), api.me()]);
  store.setPrefs(prefs);
  store.setMe(me);
  const ssn = (me && me.session) || {};
  const roleLabel = { STUDENT: "学生端", TEACHER: "教师端", ADMIN: "教务端" }[ssn.role] || ssn.role || "—";
  const identityLine = ssn.role === "STUDENT"
    ? `学号 ${ssn.studentNo || "—"} · ${ssn.major || "—"} · ${ssn.grade || "—"}`
    : ssn.role === "TEACHER"
      ? `工号 ${ssn.teacherNo || "—"} · ${ssn.title || "—"} · ${ssn.dept || "—"}`
      : `管理账号 ${ssn.username || "—"}`;
  const s = store.get();
  const accounts = Array.isArray(me && me.accounts) ? me.accounts : [];

  view.innerHTML = `
    ${pageHead("个人中心", "偏好设置与个性化")}
    <div class="card">
      <h2 style="font-size:var(--fs-h2);margin-bottom:12px">外观</h2>
      <div class="f-group"><span class="f-label">主题模式</span>
        <div class="chips" id="themeChips">
          <button class="chip ${s.prefs.theme === "light" ? "on" : ""}" data-v="light">浅色</button>
          <button class="chip ${s.prefs.theme === "dark" ? "on" : ""}" data-v="dark">深色</button>
          <button class="chip ${s.prefs.theme === "system" ? "on" : ""}" data-v="system">跟随系统</button>
        </div>
        <p class="hint-text">"跟随系统"会自动适配操作系统深浅色外观；手动选择浅色 / 深色可覆盖系统设置。</p>
      </div>
      <div class="f-group" style="margin-top:14px"><span class="f-label">字体大小（四档，即时生效）</span>
        <div class="chips" id="fontChips">
          <button class="chip ${s.prefs.fontScale === "small" ? "on" : ""}" data-v="small">小</button>
          <button class="chip ${!s.prefs.fontScale || s.prefs.fontScale === "standard" ? "on" : ""}" data-v="standard">标准</button>
          <button class="chip ${s.prefs.fontScale === "large" ? "on" : ""}" data-v="large">大</button>
          <button class="chip ${s.prefs.fontScale === "xlarge" ? "on" : ""}" data-v="xlarge">特大</button>
        </div>
      </div>
      <div class="f-group" style="margin-top:14px"><span class="f-label">信息密度</span>
        <div class="chips" id="densityChips">
          <button class="chip ${s.prefs.density === "compact" ? "on" : ""}" data-v="compact">紧凑</button>
          <button class="chip ${s.prefs.density === "standard" ? "on" : ""}" data-v="standard">标准</button>
          <button class="chip ${s.prefs.density === "loose" ? "on" : ""}" data-v="loose">宽松</button>
        </div>
      </div>
      <div class="f-group" style="margin-top:14px"><span class="f-label">课表默认视图</span>
        <div class="chips" id="viewChips">
          <button class="chip ${s.prefs.timetableView === "week" ? "on" : ""}" data-v="week">周视图</button>
          <button class="chip ${s.prefs.timetableView === "list" ? "on" : ""}" data-v="list">列表视图</button>
        </div>
      </div>
    </div>
    <div class="card">
      <h2 style="font-size:var(--fs-h2);margin-bottom:12px">登录身份${accounts.length > 1 ? `（${accounts.length} 个在读身份）` : ""}</h2>
      <div class="acct-item cur">
        <div class="avatar">${esc((ssn.realName || ssn.username || "?").slice(0, 1))}</div>
        <div class="meta">
          <div class="n">${esc(ssn.realName || ssn.username || "—")} <span class="tag pri">${esc(roleLabel)}</span></div>
          <div class="s">${esc(identityLine)}</div>
        </div>
      </div>
      ${accounts.length > 1 ? `
        <div class="acct-list">
          ${accounts.map((a, i) => `
            <div class="acct-item ${a.current ? "cur" : ""}">
              <div class="avatar">${esc(String(a.name || "?").slice(0, 1))}</div>
              <div class="meta">
                <div class="n">${esc(a.name || "—")}${a.current ? ` <span class="tag ok">当前登录</span>` : ""}</div>
                <div class="s">学号 ${esc(a.studentNo || "—")} · 已选 ${Number(a.enrolledCount || 0)} 门</div>
              </div>
              ${a.current ? "" : `<button class="btn sm" type="button" data-switch="${i}">切换登录</button>`}
            </div>`).join("")}
        </div>
        <p class="hint-text">同一自然人可能持有多个身份（如主修 + 辅修）。切换身份会先退出当前会话，再用目标账号重新登录，避免跨身份串用登录态。</p>` : ""}
      <p class="hint-text">账号密码存于本地 MySQL（sys_user 表），三个端各自使用独立账号密码登录；接口按端鉴权，学生 / 教师 / 教务互不可见对方功能。</p>
      <button class="btn" id="logoutHere" style="margin-top:12px">退出登录</button>
    </div>
    <div class="card">
      <h2 style="font-size:var(--fs-h2);margin-bottom:8px">说明</h2>
      <p style="margin:0;color:var(--tx-2);font-size:var(--fs-2)">
        深色模式遵循 WCAG AA 对比度；正文最小 16px、次要最小 14px、最小触控热区 44px。
        设置会实时保存到后端 <span class="mono">PUT /api/preferences</span>。
      </p>
    </div>`;

  const bindChips = (id, key) => {
    view.querySelector(id).addEventListener("click", async (e) => {
      const btn = e.target.closest(".chip"); if (!btn) return;
      view.querySelector(id).querySelectorAll(".chip").forEach((x) => x.classList.toggle("on", x === btn));
      store.setPrefs({ [key]: btn.dataset.v });
      await api.setPrefs({ [key]: btn.dataset.v });
      toast("已保存", "", "ok", 1200);
    });
  };
  bindChips("#themeChips", "theme");
  bindChips("#fontChips", "fontScale");
  bindChips("#densityChips", "density");
  bindChips("#viewChips", "timetableView");

  view.querySelectorAll("[data-switch]").forEach((b) => (b.onclick = () => {
    const a = accounts[Number(b.dataset.switch)];
    /* 切换身份统一走 main.js 的会话流程（确认 → 退出 → 回登录页预填账号），避免各视图各写一套 */
    window.dispatchEvent(new CustomEvent("cp:switch-identity", { detail: a }));
  }));

  const lo = view.querySelector("#logoutHere");
  if (lo) lo.onclick = async () => {
    try { await api.logout(); } catch { /* token 已失效也无妨 */ }
    api.auth.setToken(null);
    toast("已退出登录", "正在返回登录页…", "ok", 1200);
    setTimeout(() => location.reload(), 600);
  };
}

/* ================= 架构视图 ================= */
export async function renderArch(view) {
  view.innerHTML = pageHead("架构视图", "前后端分离结构 · 接口清单 · 实时请求日志") + skeletonList(1);
  const { data } = await api.architecture();

  view.innerHTML = `
    ${pageHead("架构视图", "前后端分离结构 · 接口清单 · 实时请求日志")}
    <div class="card">
      <h2 style="font-size:var(--fs-h2);margin-bottom:10px">请求链路</h2>
      <div class="flow">
        <span class="box">浏览器（前端 client/）</span><span class="arrow">→</span>
        <span class="box mono">fetch /api/*</span><span class="arrow">→</span>
        <span class="box">HTTP 服务（Spring Boot · Tomcat 8080）</span><span class="arrow">→</span>
        <span class="box">路由（controller/*）</span><span class="arrow">→</span>
        <span class="box">业务（service/*）</span><span class="arrow">→</span>
        <span class="box">数据（MySQL + MyBatis-Plus）</span>
      </div>
    </div>
    <div class="card">
      <h2 style="font-size:var(--fs-h2);margin-bottom:10px">接口清单（GET 与写操作）</h2>
      <div class="tt-wrap"><table class="api-table"><thead><tr><th>方法</th><th>路径</th><th>说明</th><th>所属层</th></tr></thead>
      <tbody>${data.endpoints.map((e) => `
        <tr><td><span class="method m-${e.method.toLowerCase()}">${e.method}</span></td>
        <td class="mono">${esc(e.path)}</td><td>${esc(e.desc)}</td><td>${esc(e.module)}</td></tr>`).join("")}
      </tbody></table></div>
    </div>
    <div class="card">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px">
        <h2 style="font-size:var(--fs-h2)">实时请求日志</h2>
        <span class="tag pri" id="logCount">0 条</span>
      </div>
      <div class="log" id="reqLog">暂无请求…</div>
      <p style="font-size:var(--fs-2);color:var(--tx-3);margin:8px 0 0">包含页面触发的所有 fetch 请求与 SSE 名额推送，可直观看到前后端交互。</p>
    </div>`;

  api.onLog((log) => {
    const node = document.getElementById("reqLog");
    const cnt = document.getElementById("logCount");
    if (!node) return;
    if (cnt) cnt.textContent = log.length + " 条";
    node.innerHTML = log.length
      ? log.map((r) => `<div>[${r.at}] <b>${r.method}</b> ${esc(r.path)} → <b>${r.status || "ERR"}</b> ${r.ms}ms${r.note ? " · " + esc(r.note) : ""}${r.error ? " · " + esc(r.error) : ""}</div>`).join("")
      : "暂无请求…";
  });
}

/* ================= 教师工作台 ================= */
export async function renderTeacher(view) {
  view.innerHTML = pageHead("教师工作台", "我的授课 · 选课名单 · 信息维护") + skeletonList(2);
  const [me, courses] = await Promise.all([api.teacherMe(), api.teacherCourses()]);
  const t = me.data;
  const items = courses.data.items;

  view.innerHTML = `
    ${pageHead("教师工作台", "我的授课 · 选课名单 · 信息维护")}
    <div class="grid g4" style="margin-bottom:14px">
      <div class="stat"><div class="v">${t.courseCount}</div><div class="l">授课门数</div></div>
      <div class="stat"><div class="v">${t.totalStudents}</div><div class="l">选课学生总数</div></div>
      <div class="stat"><div class="v">${t.fillRate}%</div><div class="l">平均满班率</div></div>
      <div class="stat"><div class="v">${t.remaining}</div><div class="l">剩余名额总数</div></div>
    </div>
    <div class="card" style="display:flex;justify-content:space-between;align-items:center">
      <div><b>${esc(t.name)}</b> · ${esc(t.title)} · ${esc(t.dept)}</div>
    </div>
    ${items.length ? items.map((c) => `
      <div class="course-card">
        <div class="cc-main">
          <div class="cc-title"><span class="cc-name">${esc(c.name)}</span><span class="cc-code">${esc(c.code)}</span>
            <span class="tag">${esc(c.category)}</span>${c.remaining === 0 ? '<span class="tag danger">已满</span>' : c.remaining <= 8 ? '<span class="tag warn">名额紧张</span>' : ""}
          </div>
          <div class="cc-meta">
            <span><b>时间</b> ${esc(scheduleText(c))}</span>
            <span><b>地点</b> ${esc(c.campus)} ${esc(c.place)}</span>
            <span><b>学分</b> ${c.credits}</span>
            <span><b>考核</b> ${esc(c.assessment)}</span>
          </div>
        </div>
        <div class="cc-side">
          <div class="seat">
            <div class="n ${c.remaining <= 8 ? "low" : ""}">${c.enrolled}</div>
            <div class="c">/ ${c.capacity} 已选</div>
            <div class="seat-bar"><i class="${c.remaining <= 8 ? "low" : ""}" style="width:${c.fillRate}%"></i></div>
          </div>
          <div style="display:flex;gap:6px;flex-wrap:wrap;justify-content:flex-end">
            <button class="btn sm" data-roster="${c.id}">${icon("courses", 16)} 名单</button>
            <button class="btn sm" data-export="${c.id}">${icon("down", 16)} 导出</button>
            <button class="btn sm" data-edit="${c.id}">${icon("settings", 16)} 编辑</button>
          </div>
        </div>
      </div>`).join("") : emptyBox("暂无授课课程")}
  `;

  view.querySelectorAll("[data-roster]").forEach((b) => (b.onclick = () => openRosterModal(Number(b.dataset.roster))));
  view.querySelectorAll("[data-export]").forEach((b) => (b.onclick = () => {
    toast("正在导出名单", "", "ok", 1500);
    window.open(`/api/teacher/courses/${b.dataset.export}/roster.csv?token=${encodeURIComponent(api.getToken() || "")}`, "_blank");
  }));
  view.querySelectorAll("[data-edit]").forEach((b) => (b.onclick = () => openEditModal(Number(b.dataset.edit), view)));
}

async function openRosterModal(id) {
  const { data } = await api.roster(id);
  if (data.error) { toast("无法查看", data.error, "danger"); return; }
  const c = data.course;
  openModal({
    title: `${c.name} · 选课名单`,
    body: `
      <p style="margin:0 0 10px;color:var(--tx-2);font-size:var(--fs-2)">${esc(c.code)} · ${esc(scheduleText(c))} · 共 ${data.students.length} 人</p>
      <div class="tt-wrap" style="max-height:50vh;overflow:auto">
        <table class="api-table"><thead><tr><th>学号</th><th>姓名</th><th>年级</th><th>专业</th><th>状态</th></tr></thead>
        <tbody>${data.students.map((s) => `<tr><td class="mono">${esc(s.studentNo)}</td><td>${esc(s.name)}</td><td>${esc(s.grade)}</td><td>${esc(s.major)}</td><td><span class="tag ok">${esc(s.status)}</span></td></tr>`).join("")}</tbody></table>
      </div>`,
    actions: [{ label: "关闭", kind: "primary" }],
  });
}

async function openEditModal(id, view) {
  const { data: c } = await api.course(id);
  if (!c || c.error) { toast("无法加载课程", (c && c.error) || "", "danger"); return; }
  openModal({
    title: `维护课程信息 · ${c.name}`,
    body: `
      <div class="f-group"><span class="f-label">上课地点</span><input class="inp" id="editPlace" style="width:100%" value="${esc(c.place)}"></div>
      <div class="f-group" style="margin-top:12px"><span class="f-label">课程简介</span><textarea class="inp" id="editIntro" rows="4" style="width:100%;height:auto;padding:8px">${esc(c.intro || "")}</textarea></div>
      <p style="font-size:var(--fs-2);color:var(--tx-3);margin:10px 0 0">仅可维护本人授课课程的简介与地点；容量与排课由教务统一配置。</p>`,
    actions: [
      { label: "取消" },
      {
        label: "保存", kind: "primary",
        onClick: async (node) => {
          const place = node.querySelector("#editPlace").value;
          const intro = node.querySelector("#editIntro").value;
          const r = await api.updateCourse(id, { place, intro });
          if (r.data.error) { toast("保存失败", r.data.error, "danger"); return false; }
          toast("已保存", "课程信息已更新", "ok");
          renderTeacher(view);
          return true;
        },
      },
    ],
  });
}

/* ================= 教务管理端 ================= */
const ANOMALY_TYPE = { conflict: "时间冲突", full: "名额已满", credit: "学分超限", session: "会话异常" };
const ANOMALY_TAG = { conflict: "danger", full: "warn", credit: "warn", session: "pri" };

export async function renderAdminDashboard(view) {
  view.innerHTML = pageHead("教务工作台", "总览 · 规则 · 监控 · 异常") + skeletonList(2);
  const { data: o } = await api.adminOverview();
  const r = o.rules;
  const ruleText = (b) => (b ? '<span class="tag ok">开</span>' : '<span class="tag danger">关</span>');

  view.innerHTML = `
    ${pageHead("教务工作台", "总览 · 规则 · 监控 · 异常")}
    <div class="grid g4" style="margin-bottom:14px">
      <div class="stat"><div class="v">${o.courseCount}</div><div class="l">在排课程</div></div>
      <div class="stat"><div class="v">${o.fillRate}%</div><div class="l">整体满班率</div></div>
      <div class="stat"><div class="v">${o.seatsRemaining}</div><div class="l">剩余名额</div></div>
      <div class="stat"><div class="v" style="color:${o.pendingAnomalies ? "var(--danger)" : "var(--ok)"}">${o.pendingAnomalies}</div><div class="l">待处理异常工单</div></div>
    </div>
    <div class="grid g2">
      <div class="card">
        <div style="display:flex;justify-content:space-between;margin-bottom:10px"><h2 style="font-size:var(--fs-h2)">当前选课规则</h2><button class="btn sm" data-nav="adminRules">去配置</button></div>
        <div class="tt-item"><span class="p">选课通道</span><span>${ruleText(r.selectionOpen)}</span></div>
        <div class="tt-item"><span class="p">学分上限</span><span><b>${r.creditLimit}</b> 学分</span></div>
        <div class="tt-item"><span class="p">心愿单上限</span><span><b>${r.maxWishlist}</b> 门</span></div>
        <div class="tt-item"><span class="p">允许跨校区</span><span>${ruleText(r.allowCrossCampus)}</span></div>
        <div class="tt-item"><span class="p">冲突硬性阻断</span><span>${ruleText(r.blockOnConflict)}</span></div>
      </div>
      <div class="card">
        <div style="display:flex;justify-content:space-between;margin-bottom:10px"><h2 style="font-size:var(--fs-h2)">运行概况</h2><button class="btn sm" data-nav="adminMonitor">去监控</button></div>
        <div class="tt-item"><span class="p">累计请求</span><span><b>${o.totalRequests}</b> 次</span></div>
        <div class="tt-item"><span class="p">服务运行</span><span><b>${Math.floor(o.uptime / 60)} 分 ${o.uptime % 60} 秒</b></span></div>
        <div class="tt-item"><span class="p">总容量</span><span><b>${o.totalCapacity}</b></span></div>
        <div class="tt-item"><span class="p">已选人次</span><span><b>${o.totalEnrolled}</b></span></div>
        <div style="margin-top:10px"><button class="btn sm" data-nav="adminAnomalies">处理异常工单 →</button></div>
      </div>
    </div>`;
  view.querySelectorAll("[data-nav]").forEach((b) => (b.onclick = () => (location.hash = "#/" + b.dataset.nav)));
}

export async function renderAdminRules(view) {
  view.innerHTML = pageHead("规则配置", "实时生效，直接影响学生端选课行为") + skeletonList(1);
  const [{ data: r }, { data: pd }] = await Promise.all([api.adminRules(), api.periods()]);
  const periods = (pd && pd.items) || [];

  const sw = (key, label, desc) => `
    <div class="tt-item" style="align-items:center">
      <div style="flex:1"><b>${label}</b><div style="font-size:var(--fs-2);color:var(--tx-3)">${desc}</div></div>
      <label class="switch"><input type="checkbox" data-rule="${key}" ${r[key] ? "checked" : ""}></label>
    </div>`;
  const num = (key, label, desc, min, max) => `
    <div class="tt-item" style="align-items:center">
      <div style="flex:1"><b>${label}</b><div style="font-size:var(--fs-2);color:var(--tx-3)">${desc}</div></div>
      <input class="inp" type="number" min="${min}" max="${max}" data-rule="${key}" value="${r[key]}" style="width:96px">
    </div>`;

  /* P-01-1 选课阶段时间：localDateTime 直接喂给 datetime-local（格式 yyyy-MM-ddTHH:mm） */
  const toLocalInput = (v) => String(v || "").slice(0, 16).replace(" ", "T");
  const periodRow = (p) => {
    const tag = p.current ? "进行中" : p.status === "FINISHED" ? "已结束" : "未开始";
    const cls = p.current ? "ongoing" : p.status === "FINISHED" ? "finished" : "pending";
    return `
    <div class="period-edit ${cls}" data-period="${p.code}">
      <div class="pe-head">
        <b>${esc(p.name)}</b>
        <span class="phase-tag">${tag}</span>
        ${p.remark ? `<span style="color:var(--tx-3);font-size:var(--fs-2)">${esc(p.remark)}</span>` : ""}
      </div>
      <div class="pe-fields">
        <label>开始<input class="inp" type="datetime-local" data-p-start value="${toLocalInput(p.start)}"></label>
        <label>结束<input class="inp" type="datetime-local" data-p-end value="${toLocalInput(p.end)}"></label>
        <button class="btn sm" data-p-save="${p.code}">${icon("check", 16)} 保存该阶段</button>
      </div>
    </div>`;
  };

  view.innerHTML = `
    ${pageHead("规则配置", "实时生效，直接影响学生端选课行为")}
    <div class="card">
      ${sw("selectionOpen", "选课通道", "关闭后学生无法提交选课（心愿单仍可维护）")}
      ${num("creditLimit", "学分上限", "学生本学期已选学分不得超过该值", 6, 40)}
      ${num("maxWishlist", "心愿单上限", "心愿单最多容纳课程数", 1, 20)}
      ${sw("allowCrossCampus", "允许跨校区选课", "关闭后跨校区连堂将被视为硬性冲突")}
      ${sw("blockOnConflict", "时间冲突硬性阻断", "关闭后时间冲突仅作警告，不阻断选课")}
      <div style="margin-top:14px;display:flex;gap:8px;justify-content:flex-end">
        <button class="btn" id="ruleReset">重置演示数据</button>
        <button class="btn primary" id="ruleSave">${icon("check", 18)} 保存规则</button>
      </div>
    </div>

    <div class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:4px">
        <h2 style="font-size:var(--fs-h2)">选课阶段时间</h2>
        <span class="tag" style="color:var(--tx-3);font-size:var(--fs-2)">数据源：selection_period</span>
      </div>
      <div style="font-size:var(--fs-2);color:var(--tx-3);margin-bottom:12px">
        这里是学生端倒计时的唯一依据。把某个阶段改到未来，学生端倒计时会立刻跟着变，无需重启服务。
      </div>
      ${periods.length ? periods.map(periodRow).join("") : emptyBox("尚未配置选课阶段", "请先在数据库中初始化 selection_period 表")}
    </div>

    <div class="callout warn">
      <b>提示　</b>修改后无需重启即可生效：例如把「学分上限」调低，学生端立即按新上限校验；把「选课通道」关闭，学生提交会被拒绝并提示教务已关闭；调整「选课阶段时间」会直接改变学生端看到的倒计时。
    </div>`;

  view.querySelector("#ruleSave").onclick = async () => {
    const patch = {};
    view.querySelectorAll("[data-rule]").forEach((inp) => {
      patch[inp.dataset.rule] = inp.type === "checkbox" ? inp.checked : Number(inp.value);
    });
    await api.setAdminRules(patch);
    toast("规则已保存", "已实时生效", "ok");
    renderAdminRules(view);
  };
  view.querySelector("#ruleReset").onclick = async () => {
    const ok = await confirmDialog("重置演示数据", "将恢复课程名额、已选课程、心愿单、票据与监控指标到初始状态，确定继续吗？");
    if (!ok) return;
    await api.adminReset();
    toast("已重置", "演示数据已恢复初始", "ok");
    renderAdminRules(view);
    refreshBadge();
  };
  /* 逐个阶段保存：只提交该行的起止时间 */
  view.querySelectorAll("[data-p-save]").forEach((btn) => (btn.onclick = async () => {
    const row = btn.closest("[data-period]");
    const startTime = row.querySelector("[data-p-start]").value;
    const endTime = row.querySelector("[data-p-end]").value;
    if (!startTime || !endTime) { toast("时间不完整", "请同时填写开始与结束时间", "warn"); return; }
    const res = await api.setPeriod(btn.dataset.pSave, {
      startTime: startTime.replace("T", " "),
      endTime: endTime.replace("T", " "),
    });
    if (res.data.error) { toast("保存失败", res.data.error, "warn"); return; }
    toast("阶段已更新", "学生端倒计时已按新时间刷新", "ok");
    renderAdminRules(view);
  }));
}

export async function renderAdminMonitor(view) {
  view.innerHTML = pageHead("运行监控", "请求量 · SSE 连接 · 内存 · 票据") + skeletonList(2);

  async function paint() {
    const { data: m } = await api.adminMonitor();
    const set = (id, v) => { const n = document.getElementById(id); if (n) n.textContent = v; };
    const top = document.getElementById("monTop");
    if (!top) return;
    top.innerHTML = `
      <div class="stat"><div class="v">${m.totalRequests}</div><div class="l">累计请求</div></div>
      <div class="stat"><div class="v" style="color:${m.errors ? "var(--danger)" : "var(--ok)"}">${m.errors}</div><div class="l">异常请求</div></div>
      <div class="stat"><div class="v">${m.sseClients}</div><div class="l">SSE 实时连接</div></div>
      <div class="stat"><div class="v">${m.memoryMB} MB</div><div class="l">内存占用</div></div>`;
    set("monUptime", `${Math.floor(m.uptime / 60)} 分 ${m.uptime % 60} 秒 · ${m.node}`);
    const maxPath = Math.max(1, ...m.byPath.map((x) => x.count));
    const bp = document.getElementById("monByPath");
    if (bp) bp.innerHTML = m.byPath.length ? m.byPath.map((x) => `
      <div style="margin-bottom:8px">
        <div style="display:flex;justify-content:space-between;font-size:var(--fs-2)"><span class="mono">${esc(x.path)}</span><span>${x.count}</span></div>
        <div class="seat-bar" style="width:100%"><i style="width:${Math.round((x.count / maxPath) * 100)}%"></i></div>
      </div>`).join("") : '<div style="color:var(--tx-3)">暂无请求记录</div>';
    const tt = document.getElementById("monTickets");
    if (tt) tt.innerHTML = m.recentTickets.length ? m.recentTickets.map((t) => `
      <div class="tt-item"><span class="p mono">${esc(t.id)}</span>
      <span>${["成功", "失败"].includes(t.status) ? `<span class="tag ${t.status === "成功" ? "ok" : "danger"}">${esc(t.status)}</span>` : `<span class="tag warn">${esc(t.status)}</span>`} 成功 ${t.accepted} / 失败 ${t.rejected}（共 ${t.total}）</span></div>`).join("")
      : '<div style="color:var(--tx-3)">暂无选课票据</div>';
    const meta = document.getElementById("monMeta");
    if (meta) meta.textContent = `票据 ${m.tickets} · 心愿单 ${m.wishlistSize} · 已选 ${m.enrolledCount} 门`;
  }

  view.innerHTML = `
    ${pageHead("运行监控", "请求量 · SSE 连接 · 内存 · 票据")}
    <div class="grid g4" id="monTop" style="margin-bottom:14px"></div>
    <div class="card" style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:8px">
      <div>运行时长：<b id="monUptime">--</b></div>
      <div id="monMeta" style="color:var(--tx-2);font-size:var(--fs-2)"></div>
      <button class="btn sm" id="monRefresh">${icon("refresh", 16)} 刷新</button>
    </div>
    <div class="grid g2">
      <div class="card"><h2 style="font-size:var(--fs-h2);margin-bottom:10px">接口调用排行</h2><div id="monByPath"></div></div>
      <div class="card"><h2 style="font-size:var(--fs-h2);margin-bottom:10px">最近选课票据</h2><div id="monTickets"></div></div>
    </div>`;

  await paint();
  view.querySelector("#monRefresh").onclick = paint;
  tick(paint, 3000); // 每 3s 自动刷新，切页时由 clearViewTimers 清理
}

export async function renderAdminAnomalies(view) {
  view.innerHTML = pageHead("异常处理", "选课异常工单 · 强制补选 / 标记处理") + skeletonList(2);
  let status = "";

  async function paint() {
    const { data } = await api.adminAnomalies(status);
    const list = data.items;
    const body = document.getElementById("anomList");
    if (!body) return;
    document.getElementById("anomCount").textContent = `${list.length} 条`;
    body.innerHTML = list.length ? list.map((a) => `
      <div class="wl-item">
        <div class="wl-body">
          <div class="wl-name">${esc(a.courseName)} <span class="tag ${ANOMALY_TAG[a.type] || ""}">${esc(ANOMALY_TYPE[a.type] || a.type)}</span>
            ${a.status === "resolved" ? '<span class="tag ok">已处理</span>' : '<span class="tag warn">待处理</span>'}</div>
          <div class="wl-sub">${esc(a.student)} · ${esc(a.time)}</div>
          <div class="wl-warn">${icon("info", 14)} ${esc(a.reason)}</div>
          ${a.resolution ? `<div class="wl-sub" style="color:var(--ok)">${icon("check", 14)} ${esc(a.resolution)}</div>` : ""}
        </div>
        ${a.status === "pending" ? `<div style="display:flex;gap:6px;flex-wrap:wrap;justify-content:flex-end">
          ${a.courseId ? `<button class="btn sm primary" data-force="${a.id}">${icon("check", 16)} 强制补选</button>` : ""}
          <button class="btn sm" data-dismiss="${a.id}">标记已处理</button>
        </div>` : ""}
      </div>`).join("") : emptyBox("暂无异常工单");

    body.querySelectorAll("[data-force]").forEach((b) => (b.onclick = async () => {
      const ok = await confirmDialog("强制补选", "将为该学生强制选入该课程（可能超出容量），确定吗？");
      if (!ok) return;
      const r = await api.resolveAnomaly(b.dataset.force, "force");
      if (r.data.error) toast("处理失败", r.data.error, "danger");
      else { toast("已强制补选", r.data.anomaly.resolution, "ok"); paint(); refreshBadge(); }
    }));
    body.querySelectorAll("[data-dismiss]").forEach((b) => (b.onclick = async () => {
      await api.resolveAnomaly(b.dataset.dismiss, "dismiss");
      toast("已标记为处理完成", "", "ok");
      paint();
    }));
  }

  view.innerHTML = `
    ${pageHead("异常处理", "选课异常工单 · 强制补选 / 标记处理")}
    <div class="card" style="display:flex;justify-content:space-between;align-items:center">
      <div class="chips" id="anomChips">
        <button class="chip on" data-s="">全部</button>
        <button class="chip" data-s="pending">待处理</button>
        <button class="chip" data-s="resolved">已处理</button>
      </div>
      <span class="tag pri" id="anomCount">0 条</span>
    </div>
    <div id="anomList"></div>`;

  view.querySelector("#anomChips").addEventListener("click", (e) => {
    const b = e.target.closest(".chip"); if (!b) return;
    status = b.dataset.s;
    view.querySelectorAll("#anomChips .chip").forEach((x) => x.classList.toggle("on", x === b));
    paint();
  });

  await paint();
}

/* ---------- 角标刷新 ---------- */
export async function refreshBadge() {
  const [wish, msgs] = await Promise.all([api.wishlist(), api.messages()]);
  const counts = {
    wishlist: wish.data.items.length,
    messages: msgs.data.items.filter((m) => !m.read).length,
  };
  if (document.querySelector('[data-badge="anomalies"]')) {
    const a = await api.adminAnomalies("pending");
    counts.anomalies = a.data.items.length;
  }
  document.querySelectorAll("[data-badge]").forEach((n) => {
    const v = counts[n.dataset.badge] || 0;
    n.textContent = v;
    n.style.display = v > 0 ? "inline-flex" : "none";
  });
}
