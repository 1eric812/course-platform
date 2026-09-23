/* 后端 · 数据层：种子数据 + 内存存储 + 原子名额扣减
 * 生产环境应替换为数据库；此处用内存态便于演示，单线程事件循环保证扣减原子性。 */

// code, 名称, 类别, 学分, 教师, 校区, 地点, 排课("日:起-止"), 考核, 容量, 评分
const SEED = [
  ["CS101", "数据结构与算法", "必修", 4, "张伟", "中心校区", "信息楼 A301", "1:1-2/3:3-4", "考试", 120, 4.8],
  ["CS102", "操作系统原理", "必修", 3, "李静", "中心校区", "信息楼 A402", "2:1-2/4:6-7", "考试", 100, 4.5],
  ["CS103", "计算机网络", "必修", 3, "王强", "中心校区", "信息楼 B205", "3:1-2/5:3-4", "考试", 100, 4.2],
  ["CS104", "数据库系统", "必修", 3, "陈磊", "中心校区", "信息楼 B301", "1:6-7/4:1-2", "考试", 110, 4.6],
  ["CS201", "机器学习导论", "选修", 3, "刘洋", "东校区", "理科楼 C101", "2:3-4", "论文", 80, 4.9],
  ["CS202", "计算机图形学", "选修", 2, "赵敏", "东校区", "理科楼 C203", "4:3-4", "考查", 60, 4.1],
  ["CS203", "软件工程", "选修", 3, "孙涛", "东校区", "理科楼 C305", "5:1-2", "论文", 70, 4.3],
  ["CS204", "编译原理", "选修", 3, "周华", "中心校区", "信息楼 A505", "3:6-7/5:6-7", "考试", 50, 3.9],
  ["CS205", "分布式系统", "选修", 3, "吴迪", "中心校区", "信息楼 A601", "2:6-7", "论文", 45, 4.7],
  ["CS206", "信息安全基础", "选修", 2, "郑凯", "东校区", "理科楼 D102", "1:3-4", "考查", 65, 4.0],
  ["CS207", "人机交互设计", "选修", 2, "冯琳", "东校区", "理科楼 D204", "4:8-9", "考查", 55, 4.6],
  ["CS208", "自然语言处理", "选修", 3, "何军", "中心校区", "信息楼 B508", "5:3-4", "论文", 40, 4.8],
  ["CS209", "深度学习进阶", "选修", 3, "刘洋", "中心校区", "信息楼 A702", "3:8-9", "论文", 45, 4.9],
  ["CS210", "强化学习", "选修", 2, "刘洋", "东校区", "理科楼 C401", "5:8-9", "考查", 40, 4.8],
  ["MA101", "高等数学（下）", "必修", 5, "许静", "中心校区", "教学楼 101", "1:1-2/3:1-2/5:1-2", "考试", 200, 4.0],
  ["MA102", "线性代数", "必修", 3, "范平", "中心校区", "教学楼 203", "2:3-4/4:3-4", "考试", 180, 4.4],
  ["MA103", "概率论与数理统计", "必修", 3, "曹宇", "中心校区", "教学楼 305", "1:8-9/3:8-9", "考试", 160, 4.2],
  ["MA201", "离散数学", "选修", 3, "姜涛", "东校区", "理科楼 A102", "2:8-9", "考试", 90, 4.1],
  ["MA202", "数值分析", "选修", 2, "谢明", "东校区", "理科楼 A204", "4:1-2", "考查", 70, 3.8],
  ["PH101", "大学物理（下）", "必修", 4, "孔亮", "中心校区", "实验楼 101", "2:6-7/5:6-7", "考试", 150, 3.7],
  ["PH102", "大学物理实验", "必修", 1, "华强", "中心校区", "实验楼 205", "3:10-12", "考查", 150, 4.3],
  ["EN101", "学术英语写作", "必修", 2, "Emily Chen", "中心校区", "外语楼 301", "1:3-4", "考查", 120, 4.5],
  ["EN102", "英语口语与演讲", "必修", 2, "John Smith", "中心校区", "外语楼 402", "3:3-4", "考查", 100, 4.7],
  ["EN201", "第二外语（日语）", "通识", 2, "山田樱", "东校区", "外语楼 105", "4:6-7", "考查", 60, 4.6],
  ["EN202", "第二外语（法语）", "通识", 2, "Lucie Martin", "东校区", "外语楼 107", "5:8-9", "考查", 45, 4.4],
  ["GE201", "中国古代文学", "通识", 2, "沈从文", "中心校区", "文法楼 201", "1:6-7", "论文", 100, 4.8],
  ["GE202", "西方哲学史", "通识", 2, "顾准", "中心校区", "文法楼 303", "2:1-2", "论文", 90, 4.5],
  ["GE203", "艺术鉴赏", "通识", 2, "林风眠", "东校区", "艺术楼 101", "3:6-7", "考查", 110, 4.9],
  ["GE204", "音乐基础", "通识", 2, "冼星海", "东校区", "艺术楼 203", "4:3-4", "考查", 80, 4.7],
  ["GE205", "心理学导论", "通识", 2, "潘菽", "中心校区", "文法楼 405", "1:8-9", "论文", 130, 4.6],
  ["GE206", "经济学原理", "通识", 3, "厉以宁", "中心校区", "经管楼 101", "5:1-2", "考试", 140, 4.3],
  ["GE207", "社会学导论", "通识", 2, "费孝通", "中心校区", "文法楼 502", "2:8-9", "论文", 100, 4.2],
  ["GE208", "环境与可持续发展", "通识", 2, "曲格平", "东校区", "环科楼 101", "3:8-9", "考查", 95, 4.0],
  ["GE209", "逻辑与批判性思维", "通识", 2, "金岳霖", "中心校区", "文法楼 601", "5:6-7", "论文", 85, 4.4],
  ["PE101", "篮球", "体育", 1, "姚明", "中心校区", "体育馆", "2:10-11", "考查", 40, 4.8],
  ["PE102", "羽毛球", "体育", 1, "林丹", "中心校区", "体育馆", "3:10-11", "考查", 36, 4.9],
  ["PE103", "瑜伽", "体育", 1, "张蕙兰", "东校区", "体操房", "1:10-11", "考查", 30, 4.7],
  ["PE104", "游泳", "体育", 1, "孙杨", "中心校区", "游泳馆", "4:10-11", "考查", 32, 4.9],
  ["PE105", "乒乓球", "体育", 1, "马龙", "东校区", "球类馆", "5:10-11", "考查", 30, 4.6],
  ["PE106", "太极", "体育", 1, "陈小旺", "中心校区", "武术馆", "2:3-4", "考查", 28, 4.5],
];

export const CATEGORIES = ["必修", "选修", "通识", "体育"];
export const ASSESSMENTS = ["考试", "论文", "考查"];

/** 部分课程的先修要求（演示 I-02「先修缺失」软性冲突：未选先修课即提示） */
const PREREQ = {
  CS201: "线性代数",
  CS203: "数据结构与算法",
  CS204: "数据结构与算法",
  CS208: "机器学习导论",
  CS209: "机器学习导论",
  CS210: "机器学习导论",
  MA201: "高等数学（下）",
  MA202: "概率论与数理统计",
  PH102: "大学物理（下）",
};

/** "1:1-2/3:3-4" -> [{day:1,start:1,end:2},{day:3,start:3,end:4}] */
function parseSchedule(s) {
  return s.split("/").map((part) => {
    const [day, range] = part.split(":");
    const [start, end] = range.split("-").map(Number);
    return { day: Number(day), start, end };
  });
}

function buildCourses() {
  let seed = 20260920; // 固定伪随机，保证每次启动数据一致
  const rnd = () => ((seed = (seed * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff);
  return SEED.map((r, i) => {
    const [code, name, category, credits, teacher, campus, place, sched, assessment, capacity, rating] = r;
    const enrolled = Math.round(capacity * (0.25 + rnd() * 0.72));
    const c = {
      id: i + 1, code, name, category, credits, teacher, campus, place,
      schedule: parseSchedule(sched), assessment, capacity, rating,
      enrolled: Math.min(enrolled, capacity),
      prereq: PREREQ[code] || null,
      tags: [],
      intro: `${name}：由${teacher}老师授课，${credits} 学分，${assessment}考核。系统介绍${name}的核心概念、方法与实践应用。`,
    };
    c.remaining = c.capacity - c.enrolled;
    refreshTags(c);
    return c;
  });
}

function refreshTags(c) {
  c.tags = c.remaining === 0 ? ["已满"] : c.remaining <= 8 ? ["名额紧张"] : [];
}

/* ---------------- 多账号（演示 I-09 会话隔离：每个账号独立状态） ---------------- */

const DEFAULT_PREFS = {
  theme: "light", density: "standard", timetableView: "week", compactFilter: false,
  fontScale: "standard", // P-08-1 字体四档：small/standard/large/xlarge
  notify: { result: true, seat: true, system: true, drop: true }, // P-07-1 消息按类型订阅
};

const WELCOME = [
  { id: 1, type: "system", title: "2026-2027 学年第一学期选课即将开始", body: "正式选课将于 9 月 25 日 09:00 开放，建议提前加入心愿单。", time: "2026-09-20 09:00", read: false },
  { id: 2, type: "result", title: "高等数学（下）选课成功", body: "你已成功选中「高等数学（下）」，请核对课表。", time: "2026-09-19 14:32", read: false },
  { id: 3, type: "seat", title: "机器学习导论剩余名额变化", body: "该课程剩余名额由 12 变为 6，请关注。", time: "2026-09-19 11:08", read: true },
];

const ACCOUNT_SEEDS = [
  { id: "a1", name: "林同学", studentNo: "2023010101", grade: "2023 级", major: "计算机科学与技术", creditLimit: 30, enrolledIds: [1, 14, 20] },
  { id: "a2", name: "苏同学", studentNo: "2023010102", grade: "2024 级", major: "视觉传达设计", creditLimit: 30, enrolledIds: [15, 31] },
  { id: "a3", name: "何同学", studentNo: "2023010103", grade: "2023 级", major: "数学与应用数学", creditLimit: 30, enrolledIds: [] },
];

function buildAccountState(s) {
  return {
    user: {
      id: s.id, name: s.name, studentNo: s.studentNo,
      grade: s.grade, major: s.major, creditLimit: s.creditLimit,
      preferences: JSON.parse(JSON.stringify(DEFAULT_PREFS)),
    },
    enrolledIds: s.enrolledIds.slice(),
    wishlist: [],        // [{courseId, priority, addedAt}]
    subscriptions: [],   // 放号订阅：courseId[]
    tickets: {},
    messages: JSON.parse(JSON.stringify(WELCOME)),
    seq: { ticket: 1000, message: 10 },
  };
}

const state = {
  courses: buildCourses(),
  currentAccountId: "a1",
  accounts: {},
};
for (const s of ACCOUNT_SEEDS) state.accounts[s.id] = buildAccountState(s);

export const db = {
  /** 当前账号的状态（getter，使业务层 db.state.xxx 自动指向当前账号） */
  get state() { return state.accounts[state.currentAccountId]; },

  allCourses: () => state.courses,
  courseById: (id) => state.courses.find((c) => c.id === Number(id)),
  user: () => state.accounts[state.currentAccountId].user,
  enrolled: () => db.state.enrolledIds.map((id) => db.courseById(id)).filter(Boolean),
  messages: () => db.state.messages,
  currentAccountId: () => state.currentAccountId,

  /** 账号列表（含当前标记） */
  accounts: () => ACCOUNT_SEEDS.map((s) => ({
    id: s.id, name: s.name, studentNo: s.studentNo, major: s.major,
    enrolledCount: state.accounts[s.id].enrolledIds.length,
    current: s.id === state.currentAccountId,
  })),

  /** 全部账号的状态对象（放号通知等需要跨账号触达的场景用） */
  allAccounts: () => Object.values(state.accounts),

  /** 切换账号：目标账号状态独立，互不串改（I-09） */
  switchAccount(id) {
    if (!state.accounts[id]) return null;
    state.currentAccountId = id;
    return db.accounts().find((a) => a.id === id);
  },

  wishlist() {
    return db.state.wishlist
      .slice()
      .sort((a, b) => a.priority - b.priority)
      .map((w) => ({ ...w, course: db.courseById(w.courseId) }))
      .filter((w) => w.course);
  },

  /** 原子扣减名额（事件循环单线程保证） */
  takeSeat(id) {
    const c = db.courseById(id);
    if (!c || c.remaining <= 0) return false;
    c.remaining -= 1;
    c.enrolled += 1;
    refreshTags(c);
    return true;
  },

  /** 放号：把名额恢复为 n（保持 enrolled + remaining = capacity），用于模拟放号与演示放号通知 */
  releaseSeat(id, n) {
    const c = db.courseById(id);
    if (!c) return null;
    c.remaining = Math.min(c.capacity, Math.max(0, n));
    c.enrolled = c.capacity - c.remaining;
    refreshTags(c);
    return c;
  },
};

/* ============================================================
 * 教师端 · 演示教师与学生名单生成器
 * ============================================================ */

/** 当前登录教师（演示用单教师），其姓名需与课程 teacher 字段一致 */
export const currentTeacher = { id: 1, name: "刘洋", title: "副教授", dept: "计算机科学与技术学院" };

const SURNAMES = "王李张刘陈杨赵黄周吴徐孙马朱胡郭何高林罗郑梁谢宋唐许韩冯邓曹彭".split("");
const GIVEN = ["伟", "芳", "娜", "敏", "静", "磊", "军", "洋", "勇", "艳", "杰", "涛", "明", "超", "雪", "晨", "宇", "浩",
  "子轩", "雨桐", "一诺", "欣怡", "浩然", "梓涵", "思远", "天翊", "若曦", "嘉懿", "沐宸", "奕辰"];

/** 按课程确定性地生成选课学生名单（同一课程多次生成结果一致） */
export function genRoster(course) {
  let seed = course.id * 7919 + 13;
  const rnd = () => ((seed = (seed * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff);
  const count = Math.min(course.enrolled, 30);
  const used = new Set();
  const list = [];
  for (let i = 0; i < count; i++) {
    let name;
    do { name = SURNAMES[(rnd() * SURNAMES.length) | 0] + GIVEN[(rnd() * GIVEN.length) | 0]; } while (used.has(name));
    used.add(name);
    list.push({
      studentNo: "202" + (3 + ((rnd() * 3) | 0)) + String(10000 + ((rnd() * 8999) | 0)),
      name,
      major: course.category === "体育" || course.category === "通识" ? "全校公选" : "计算机科学与技术",
      grade: ["2023 级", "2024 级", "2025 级"][(rnd() * 3) | 0],
      status: "已选",
    });
  }
  return list.sort((a, b) => a.studentNo.localeCompare(b.studentNo));
}

/* ============================================================
 * 教务管理端 · 规则 / 异常 / 运行指标
 * ============================================================ */

export const adminState = {
  // 可配置规则（会真实影响学生端行为）
  rules: {
    selectionOpen: true,    // 选课是否开放
    creditLimit: 30,        // 学分上限
    maxWishlist: 8,         // 心愿单上限
    allowCrossCampus: true, // 是否允许跨校区选课
    blockOnConflict: true,  // 时间冲突是否硬性阻断
  },
  // 异常工单（pending=待处理 / resolved=已处理）
  anomalies: [
    { id: 1, type: "conflict", student: "202314254 韩雨桐", courseId: 5, courseName: "机器学习导论", reason: "与「线性代数」时间冲突（周二 第 3-4 节）", time: "2026-09-21 09:12", status: "pending" },
    { id: 2, type: "full", student: "202315301 王子轩", courseId: 36, courseName: "羽毛球", reason: "名额已满（0/36）", time: "2026-09-21 09:03", status: "pending" },
    { id: 3, type: "session", student: "202312089 李浩然", courseId: null, courseName: "—", reason: "检测到同一浏览器多账号会话异常（疑似会话串号）", time: "2026-09-21 08:47", status: "pending" },
  ],
  // 运行指标
  metrics: {
    startedAt: Date.now(),
    totalRequests: 0,
    errors: 0,
    sseClients: 0,
    byPath: {}, // path -> count
  },
  seq: { anomaly: 4 },
};

/** 记录一次接口调用（供运行监控统计） */
export function recordMetric(path, isError = false) {
  const m = adminState.metrics;
  m.totalRequests += 1;
  if (isError) m.errors += 1;
  const key = path.replace(/\/\d+/g, "/:id");
  m.byPath[key] = (m.byPath[key] || 0) + 1;
}

/* ============================================================
 * 选课阶段时间表（对齐 selection_period 表）与选课规则（selection_rule 表）
 *
 * 设计口径与 server-java 保持一致：
 *  - 阶段与规则是「数据库里的配置」，不是前端写死的常量；
 *  - openAt（下一个可提交选课的时刻）由阶段表推导，而不是页面加载时间 + 90s；
 *  - 教务端可在「选课规则」里开着/关着选课开关，学生端据此禁用提交。
 * ============================================================ */

/**
 * 相对当前时刻的默认阶段编排（首次进入时按此播种，模拟教务已排好的三个阶段）。
 *
 * 注意「正选」的 offsetStart 是 -1h：演示时它**已经开放**，学生可以直接提交选课，
 * 否则默认进页面就是一个 90 秒倒计时，谁都点不了提交、测试也跑不起来。
 * 但为了保留「倒计时」这个演示点，这里让正选已经开放、而补退选尚未开始，
 * 倒计时自然指向「补退选」；同时 config.openDelayMs 仍可通过教务端把阶段
 * 时间改到未来来复现「未开放」场景。
 */
const PERIOD_SEED = [
  { phaseCode: "PRE", phaseName: "预选", offsetStart: -5 * 86400e3, offsetEnd: -3 * 86400e3, remark: "仅可加入心愿单，不做名额占用" },
  { phaseCode: "MAIN", phaseName: "正选", offsetStart: -3600e3, offsetEnd: 3 * 86400e3, remark: "按志愿序批量受理，先到先得" },
  { phaseCode: "ADJUST", phaseName: "补退选", offsetStart: 13 * 86400e3, offsetEnd: 17 * 86400e3, remark: "可退课与补选，逾期不再受理" },
];

/** "2026-09-25 09:00:00" 形状的本地时间字符串，避免时区带来的显示歧义 */
function fmtLocal(ts) {
  const d = new Date(ts);
  const p = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

function buildPeriods() {
  const now = Date.now();
  return PERIOD_SEED.map((p, i) => ({
    id: i + 1,
    phaseCode: p.phaseCode,
    phaseName: p.phaseName,
    startTime: now + p.offsetStart,
    endTime: now + p.offsetEnd,
    remark: p.remark,
  }));
}

/* ------------------------------------------------------------------
 * 阶段表持久化
 *
 * 静态版没有服务端进程，periodState 只是 JS 模块内存。教务改了阶段时间，
 * 学生端一切换账号 / 刷新页面就会重新 import 本模块、重新执行 buildPeriods()，
 * 改动被悄悄冲掉——用户会认为「教务改的时间根本没生效」。
 *
 * 因此把阶段表落到 localStorage（与 cp_session 同样的存法）：
 * 刷新后按存下来的绝对时间来还原，教务的调整才真的算数。
 * 存储不可用（隐私模式 / 配额满）时静默降级为纯内存。
 * ------------------------------------------------------------------ */
const PERIOD_KEY = "cp_periods";

function loadPeriods() {
  const fallback = buildPeriods();
  try {
    const raw = localStorage.getItem(PERIOD_KEY);
    if (!raw) return fallback;
    const saved = JSON.parse(raw);
    if (!Array.isArray(saved) || !saved.length) return fallback;
    /* 以种子为骨架，只回填时间与备注，避免旧数据缺字段导致渲染出 undefined */
    return fallback.map((base) => {
      const hit = saved.find((s) => s && s.phaseCode === base.phaseCode);
      if (!hit) return base;
      return {
        ...base,
        startTime: parseTimeInput(hit.startTime) ?? base.startTime,
        endTime: parseTimeInput(hit.endTime) ?? base.endTime,
        remark: hit.remark != null ? String(hit.remark) : base.remark,
      };
    });
  } catch (e) {
    return fallback;
  }
}

function savePeriods() {
  try {
    localStorage.setItem(PERIOD_KEY, JSON.stringify(periodState.periods));
  } catch (e) { /* 隐私模式或配额不足：降级为纯内存 */ }
}

export const periodState = {
  periods: loadPeriods(),
};

/** 阶段状态：与 DB status 字段同名，但按当前时刻实时判定（ONGOING 优先于表里的静态值） */
export function phaseStatusOf(p, now = Date.now()) {
  if (now > p.endTime) return "FINISHED";
  if (now >= p.startTime) return "ONGOING";
  return "NOT_STARTED";
}

/** 按下标时间排序后的阶段列表 */
export function sortedPeriods() {
  return periodState.periods.slice().sort((a, b) => a.startTime - b.startTime);
}

/** 当前进行中的阶段（没有则 null） */
export function currentPhase() {
  const now = Date.now();
  return sortedPeriods().find((p) => now >= p.startTime && now <= p.endTime) || null;
}

/** 下一个尚未开始的阶段（没有则 null） */
export function nextPhase() {
  const now = Date.now();
  return sortedPeriods().find((p) => p.startTime > now) || null;
}

/**
 * 「选课开放时刻」的唯一口径。
 *
 * 优先取进行中阶段的开始时间（已经开放，倒计时归零）；
 * 否则取下一个未开始阶段的开始时间；
 * 阶段表被清空时回退到「已开放」，避免前端倒计时永远转圈。
 */
export function selectionOpenAt() {
  const cur = currentPhase();
  if (cur) return cur.startTime;
  const nxt = nextPhase();
  if (nxt) return nxt.startTime;
  return Date.now();
}

/** 供 /api/status 输出的阶段视图（附 current 标记，前端直接消费） */
export function phaseViews() {
  const now = Date.now();
  return sortedPeriods().map((p) => {
    const status = phaseStatusOf(p, now);
    return {
      code: p.phaseCode,
      name: p.phaseName,
      start: fmtLocal(p.startTime),
      end: fmtLocal(p.endTime),
      status,
      current: status === "ONGOING",
      remark: p.remark,
    };
  });
}

/**
 * 把前端传来的时间值转成毫秒时间戳。
 *
 * 教务端提交的是 "yyyy-MM-dd HH:mm" / "yyyy-MM-ddTHH:mm" 这类字符串，
 * 直接 Number() 会得到 NaN；这里统一按本地时间解析，与 fmtLocal 输出对称。
 */
function parseTimeInput(v) {
  if (v == null) return null;
  if (typeof v === "number") return Number.isFinite(v) ? v : null;
  const s = String(v).trim();
  if (!s) return null;
  if (/^\d+$/.test(s)) return Number(s);
  const ts = Date.parse(s.replace(" ", "T"));
  return Number.isNaN(ts) ? null : ts;
}

/** 教务端更新阶段起止时间（演示用，允许把阶段拖到当下以观察倒计时） */
export function updatePeriod(code, patch = {}) {
  const p = periodState.periods.find((x) => x.phaseCode === code);
  if (!p) return null;
  for (const k of ["startTime", "endTime"]) {
    if (patch[k] == null) continue;
    const ts = parseTimeInput(patch[k]);
    if (ts != null) p[k] = ts;
  }
  if (patch.remark != null) p.remark = String(patch.remark);
  if (p.endTime <= p.startTime) p.endTime = p.startTime + 3600e3;
  savePeriods();
  return p;
}

/** 重置演示数据：课程名额恢复初始，当前账号的已选/心愿单/票据清空，指标清零 */
export function resetDemo() {
  state.courses = buildCourses();
  periodState.periods = buildPeriods();
  savePeriods();
  const a = db.state;
  a.enrolledIds = a.user.id === "a1" ? [1, 14, 20] : [];
  a.wishlist = [];
  a.tickets = {};
  a.subscriptions = [];
  adminState.anomalies = adminState.anomalies.filter((x) => x.id <= 3).map((x) => ({ ...x, status: "pending", resolution: undefined }));
  adminState.metrics.totalRequests = 0;
  adminState.metrics.errors = 0;
  adminState.metrics.byPath = {};
  return { ok: true };
}
