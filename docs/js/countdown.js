/* 前端 · 统一倒计时中心
 *
 * 设计要点：
 *  1. 单例驱动：全站只跑一个定时器，所有倒计时节点统一注册/注销，
 *     避免每个页面各自 setInterval 造成重复渲染与计时器泄漏。
 *  2. 绝对时间计算：每次都用 (目标时间 - Date.now()) 现算，
 *     而非每秒 self-1，后台标签页被浏览器降频后回到前台也不会漂移。
 *  3. 可见性感知：标签页隐藏时暂停定时器（省电），恢复时立即补算一次。
 *  4. 自适应格式：按剩余量自动切换 天/时/分/秒 的展示粒度。
 *  5. 分档提醒：剩余 ≤ 1 小时 / ≤ 10 分钟 / ≤ 1 分钟 分别升级视觉强调。
 *  6. 时间口径来自后端的 selection_period 阶段表（见 phases.js），
 *     本模块只负责「拿到目标时刻之后怎么显示」，不负责编造时刻。
 */

import { resolveCountdown } from "./phases.js";

const HOUR = 3600 * 1000;
const MINUTE = 60 * 1000;

const state = {
  target: null,        // 目标时间戳（选课开放时间）
  timer: null,         // 单例定时器句柄
  nodes: new Map(),    // node -> options
  listeners: new Set(),// 状态变化订阅（用于派生 UI，如高亮、文案）
  lastPhase: null,     // 上一次的阶段，用于只在跨档时通知
  lastText: new Map(), // node -> 上次写入的文本，避免无谓的 DOM 写入
  ctx: null,           // 最近一次 resolveCountdown 的语境（阶段名/区间/关闭态）
  ctxListeners: new Set(), // 语境变化订阅（阶段切换、教务改时间时刷新文案）
};

/* 允许通过 globalThis.__CP_COUNTDOWN_INTERVAL__ 调整刷新间隔（测试用） */
function tickInterval() {
  const v = Number(globalThis.__CP_COUNTDOWN_INTERVAL__);
  return Number.isFinite(v) && v > 0 ? v : 1000;
}

/* ---------- 时间格式化 ---------- */

/** 把剩余毫秒格式化为自适应文本 */
export function formatRemaining(ms) {
  if (ms <= 0) return "已开放";
  const totalSec = Math.floor(ms / 1000);
  const d = Math.floor(totalSec / 86400);
  const h = Math.floor((totalSec % 86400) / 3600);
  const m = Math.floor((totalSec % 3600) / 60);
  const s = totalSec % 60;
  const p2 = (n) => String(n).padStart(2, "0");

  if (d > 0) return `${d}天 ${p2(h)}:${p2(m)}:${p2(s)}`;
  if (h > 0) return `${p2(h)}:${p2(m)}:${p2(s)}`;
  return `${p2(m)}:${p2(s)}`;
}

/** 紧凑格式，用于顶栏等窄空间 */
export function formatCompact(ms) {
  if (ms <= 0) return "已开放";
  const totalSec = Math.floor(ms / 1000);
  const d = Math.floor(totalSec / 86400);
  const h = Math.floor((totalSec % 86400) / 3600);
  const m = Math.floor((totalSec % 3600) / 60);
  const s = totalSec % 60;
  if (d > 0) return `${d}天${h}时`;
  if (h > 0) return `${h}时${String(m).padStart(2, "0")}分`;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

/** 剩余量的紧急阶段，供样式与文案分档 */
export function phaseOf(ms) {
  if (ms <= 0) return "open";
  if (ms <= MINUTE) return "imminent";   // 最后 1 分钟
  if (ms <= 10 * MINUTE) return "soon";  // 最后 10 分钟
  if (ms <= HOUR) return "near";         // 最后 1 小时
  return "normal";
}

export const PHASE_LABEL = {
  open: "选课已开放",
  imminent: "即将开放",
  soon: "马上开放",
  near: "1 小时内开放",
  normal: "距选课开放",
};

/* ---------- 核心调度 ---------- */

function currentRemaining() {
  return state.target == null ? null : state.target - Date.now();
}

function paint() {
  const ms = currentRemaining();
  if (ms == null) return;
  const phase = phaseOf(ms);

  state.nodes.forEach((opts, node) => {
    if (!node.isConnected) {
      state.nodes.delete(node);
      state.lastText.delete(node);
      return;
    }
    const text = (opts.compact ? formatCompact : formatRemaining)(ms);
    if (node.textContent !== text) node.textContent = text;

    // 阶段类名：切档时整体替换，避免类名堆积
    if (opts.phaseClass) {
      node.dataset.phase = phase;
    }
  });

  // 仅在跨档时通知订阅者，避免每帧触发
  if (phase !== state.lastPhase) {
    state.lastPhase = phase;
    state.listeners.forEach((fn) => fn({ remaining: ms, phase }));
  }

  // 已开放后自动停表，节省资源
  if (ms <= 0) stopTimer();
}

/* 【修复】后台标签页降频导致的漂移：
 * 浏览器对隐藏标签页的 setInterval 会降到 ≥1 次/分钟，若仍按 1s 间隔计时，
 * 恢复前台时文本会落后数十秒。这里把「对齐时刻」排到下一个整秒边界，
 * 使每次刷新都紧贴秒变化点，而不是在毫秒级误差上累积。 */
function smartPause() {
  stopTimer();
  const ms = currentRemaining();
  if (ms == null || ms <= 0) return;
  const interval = tickInterval();
  const delay = Math.min(interval, Math.max(16, interval - (Date.now() % interval)));
  state.timer = setTimeout(function tick() {
    state.timer = null;
    paint();
    ensureTimer();
  }, delay);
}

function ensureTimer() {
  if (state.timer != null) return;
  if (typeof document !== "undefined" && document.hidden) return;
  state.timer = setInterval(paint, tickInterval());
}

function stopTimer() {
  if (state.timer == null) return;
  clearInterval(state.timer);
  clearTimeout(state.timer);
  state.timer = null;
}

/* 标签页可见性：隐藏时停表，恢复时立即对齐一次再续跑 */
function onVisibility() {
  if (document.hidden) {
    stopTimer();
  } else if (currentRemaining() > 0) {
    paint();        // 立即用绝对时间补算，消除后台期间的漂移
    ensureTimer();
  }
}

let visibilityBound = false;
function bindVisibility() {
  if (visibilityBound || typeof document === "undefined") return;
  document.addEventListener("visibilitychange", onVisibility);
  visibilityBound = true;
}

/* ---------- 对外 API ---------- */

/** 设置（或更新）选课开放时间，全站共用 */
export function setTarget(openAt) {
  const next = Number(openAt) || null;
  if (next === state.target) return;
  state.target = next;
  state.lastPhase = null;
  state.lastText.clear();
  bindVisibility();
  if (next == null) { stopTimer(); return; }
  paint();
  if (currentRemaining() > 0) smartPause(); else stopTimer();
}

/**
 * 用后端 /api/status 的完整载荷设置倒计时语境。
 *
 * 这是推荐的入口：它会同时确定「目标时刻」和「阶段语境」（阶段名、起止区间、
 * 教务是否关闭了通道），并只在语境真的变化时通知订阅者。
 * 相比裸 setTarget(openAt)，它不会在阶段表缺失时退化成假倒计时。
 *
 * @param {object} statusData  /api/status 的 data
 * @returns {object} 解析出的语境
 */
export function setContext(statusData) {
  const ctx = resolveCountdown(statusData);
  const prev = state.ctx;
  const changed =
    !prev ||
    prev.target !== ctx.target ||
    prev.phaseName !== ctx.phaseName ||
    prev.status !== ctx.status ||
    prev.closed !== ctx.closed;

  state.ctx = ctx;
  setTarget(ctx.target);

  if (changed) state.ctxListeners.forEach((fn) => fn(ctx));
  return ctx;
}

/** 当前语境（未设置过则为 null） */
export function getContext() { return state.ctx; }

/** 订阅语境变化（阶段切换 / 教务改时间 / 通道开关时回调），返回取消函数 */
export function onContext(fn) {
  state.ctxListeners.add(fn);
  if (state.ctx) fn(state.ctx);
  return () => state.ctxListeners.delete(fn);
}

export function getTarget() { return state.target; }
export function getRemaining() { return currentRemaining(); }

/**
 * 注册一个倒计时节点。
 * @param {HTMLElement} node 目标元素
 * @param {{compact?: boolean, phaseClass?: boolean}} [opts]
 *   compact    — 使用紧凑格式（顶栏）
 *   phaseClass — 在节点上维护 data-phase 属性，供 CSS 分档高亮
 * @returns {() => void} 注销函数
 */
export function register(node, opts = {}) {
  if (!node) return () => {};
  state.nodes.set(node, opts);
  bindVisibility();
  paint();
  if (currentRemaining() != null && currentRemaining() > 0) ensureTimer();
  return () => {
    state.nodes.delete(node);
    state.lastText.delete(node);
  };
}

/** 订阅阶段变化（跨档时才回调），返回取消订阅函数 */
export function onPhase(fn) {
  state.listeners.add(fn);
  if (state.target != null) fn({ remaining: currentRemaining(), phase: phaseOf(currentRemaining()) });
  return () => state.listeners.delete(fn);
}

/** 页面切换时清空所有节点注册（定时器若仍有目标则继续跑） */
export function unregisterAll() {
  state.nodes.clear();
  state.lastText.clear();
  state.listeners.clear();      // 页面级订阅（onPhase）随视图销毁一并回收，避免跨页串扰
  state.ctxListeners.clear();   // 语境订阅同理，否则旧页面的闭包会持有已卸载的 DOM
}

/* 阶段语境相关的再导出，方便视图层只 import 一个模块 */
export { currentPhase, nextPhase, parseTime, windowTextOf, roughEta, statusOf } from "./phases.js";

/** 测试/调试用：立刻同步一次当前剩余量（不等待下一次 tick） */
export function sync() { paint(); }
