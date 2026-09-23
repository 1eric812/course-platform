/* 前端 · UI 工具：DOM 助手、图标、Toast、Modal
 * 图标统一 2px 描边，且功能按钮均带文字（回应「图标不好辨认」）。 */

const ICONS = {
  home: '<path d="M3 10.5 12 3l9 7.5"/><path d="M5 10v10h14V10"/>',
  courses: '<path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>',
  heart: '<path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8z"/>',
  bolt: '<path d="M13 2 3 14h7l-1 8 10-12h-7l1-8z"/>',
  calendar: '<rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/>',
  bell: '<path d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.7 21a2 2 0 0 1-3.4 0"/>',
  settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.2a1.7 1.7 0 0 0-1-1.5 1.7 1.7 0 0 0-1.9.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.2a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.9l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.9.3h0a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.2a1.7 1.7 0 0 0 1 1.5h0a1.7 1.7 0 0 0 1.9-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.9v0a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.2a1.7 1.7 0 0 0-1.5 1z"/>',
  arch: '<rect x="9" y="2" width="6" height="6" rx="1"/><rect x="2" y="16" width="6" height="6" rx="1"/><rect x="16" y="16" width="6" height="6" rx="1"/><path d="M12 8v4M12 12H5v4M12 12h7v4"/>',
  moon: '<path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>',
  search: '<circle cx="11" cy="11" r="7"/><path d="M20 20l-4.3-4.3"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  trash: '<path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6M10 11v6M14 11v6"/>',
  check: '<path d="M20 6 9 17l-5-5"/>',
  refresh: '<path d="M21 12a9 9 0 1 1-3-6.7"/><path d="M21 3v6h-6"/>',
  send: '<path d="M22 2 11 13"/><path d="M22 2 15 22l-4-9-9-4 20-7z"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 3"/>',
  up: '<path d="M12 19V5M5 12l7-7 7 7"/>',
  down: '<path d="M12 5v14M5 12l7 7 7-7"/>',
  x: '<path d="M18 6 6 18M6 6l12 12"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 16v-4M12 8h.01"/>',
};

export function icon(name, size = 20) {
  return `<svg viewBox="0 0 24 24" width="${size}" height="${size}" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${ICONS[name] || ICONS.info}</svg>`;
}

export function esc(s) {
  return String(s == null ? "" : s).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

export function el(html) {
  const t = document.createElement("template");
  t.innerHTML = html.trim();
  return t.content.firstElementChild;
}

/* ---------- Toast ---------- */
export function toast(title, body = "", type = "info", ms = 2600) {
  const stack = document.getElementById("toastStack");
  const node = el(`<div class="toast ${type}"><div class="t-title">${esc(title)}</div>${body ? `<div class="t-body">${esc(body)}</div>` : ""}</div>`);
  stack.appendChild(node);
  setTimeout(() => {
    node.style.transition = "opacity .3s, transform .3s";
    node.style.opacity = "0";
    node.style.transform = "translateX(10px)";
    setTimeout(() => node.remove(), 320);
  }, ms);
}

/* ---------- Modal ---------- */
export function openModal({ title, body, actions = [] }) {
  const root = document.getElementById("modalRoot");
  const scrim = document.getElementById("scrim");
  scrim.hidden = false;
  const node = el(`
    <div class="modal" role="dialog" aria-modal="true" aria-label="${esc(title)}">
      <h2>${esc(title)}</h2>
      <div class="modal-body">${body}</div>
      <div class="modal-foot"></div>
    </div>`);
  const foot = node.querySelector(".modal-foot");
  actions.forEach((a) => {
    const btn = el(`<button class="btn ${a.kind || ""}">${esc(a.label)}</button>`);
    btn.onclick = async () => {
      let shouldClose = true;
      if (a.onClick) {
        const r = await a.onClick(node);
        if (r === false) shouldClose = false; // 回调返回 false 时保持打开（如表单校验失败）
      }
      if (shouldClose) closeModal();
    };
    foot.appendChild(btn);
  });
  root.innerHTML = "";
  root.appendChild(node);
  root.classList.add("on");
  scrim.onclick = closeModal;
  return node;
}

export function closeModal() {
  document.getElementById("modalRoot").classList.remove("on");
  document.getElementById("scrim").hidden = true;
}

export function confirmDialog(title, body) {
  return new Promise((resolve) => {
    openModal({
      title, body: `<p style="margin:0;color:var(--tx-2);font-size:var(--fs-2)">${body}</p>`,
      actions: [
        { label: "取消", onClick: () => resolve(false) },
        { label: "确认", kind: "primary", onClick: () => resolve(true) },
      ],
    });
  });
}

/* ---------- 通用渲染 ---------- */
export function seatClass(c) {
  return c.remaining === 0 ? "zero" : c.remaining <= 8 ? "low" : "";
}

export function scheduleText(c) {
  const DAYS = ["一", "二", "三", "四", "五", "六", "日"];
  return c.schedule.map((s) => `周${DAYS[s.day - 1]} ${s.start}-${s.end}节`).join(" · ");
}

export function seatHtml(c) {
  const cls = seatClass(c);
  const pct = c.capacity ? Math.round((c.remaining / c.capacity) * 100) : 0;
  return `
    <div class="seat">
      <div class="n ${cls}" data-seat="${c.id}">${c.remaining}</div>
      <div class="c">/ ${c.capacity} 名额</div>
      <div class="seat-bar"><i class="${cls}" style="width:${pct}%"></i></div>
    </div>`;
}

export function tagsHtml(c) {
  const out = [];
  out.push(`<span class="tag">${esc(c.category)}</span>`);
  if (c.remaining === 0) out.push('<span class="tag danger">已满</span>');
  else if (c.remaining <= 8) out.push('<span class="tag warn">名额紧张</span>');
  if (c.conflict) out.push('<span class="tag danger">时间冲突</span>');
  if (c.selected) out.push('<span class="tag ok">已选</span>');
  else if (c.inWishlist) out.push('<span class="tag pri">心愿单</span>');
  return out.join(" ");
}

export function pageHead(title, sub) {
  return `<div class="page-head"><h1>${esc(title)}</h1>${sub ? `<p>${esc(sub)}</p>` : ""}</div>`;
}

export function skeletonList(n = 5) {
  let s = "";
  for (let i = 0; i < n; i++) s += `<div class="card"><div class="skeleton" style="width:40%"></div><div class="skeleton" style="width:80%"></div><div class="skeleton" style="width:60%"></div></div>`;
  return s;
}

export function emptyBox(title, sub) {
  return `<div class="empty"><div class="e-ico"></div><div>${esc(title)}</div>${sub ? `<div style="font-size:var(--fs-2)">${esc(sub)}</div>` : ""}</div>`;
}
