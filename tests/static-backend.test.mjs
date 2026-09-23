// 静态版后端功能测试：直接在 Node 里跑 docs/ 的浏览器端「后端」
import { handleApi, API_CATALOG } from "../docs/js/backend/server.js";
import { db } from "../docs/js/backend/data.js";
import * as svc from "../docs/js/backend/service.js";

const call = (m, p, q = {}, b = {}) => handleApi(m, p, q, b);
let fail = 0;
const check = (name, cond, extra = "") => {
  console.log(`${cond ? "PASS" : "FAIL"}  ${name}${extra ? "  " + extra : ""}`);
  if (!cond) fail++;
};

// ---------- 基础 ----------
let r = await call("GET", "/api/health");
check("health 200", r.status === 200 && r.data.ok === true);
r = await call("GET", "/api/architecture");
check("architecture 接口清单", r.status === 200 && r.data.endpoints.length === API_CATALOG.length, `${r.data.endpoints.length} 条`);
r = await call("GET", "/api/filters");
check("filters 含新维度", r.status === 200 && r.data.days.length > 0 && r.data.periods.length > 0 && r.data.teachers.length > 0,
  `days=${r.data.days.length} periods=${r.data.periods.length} teachers=${r.data.teachers.length}`);

// ---------- 课程与筛选 ----------
r = await call("GET", "/api/courses", {});
check("courses 全量", r.status === 200 && r.data.items.length === 40, `${r.data.items.length} 门`);
const all = r.data.items.length;
r = await call("GET", "/api/courses", { onlyNoConflict: "1" });
check("onlyNoConflict 生效", r.data.items.length < all, `${r.data.items.length} < ${all}`);
r = await call("GET", "/api/courses", { days: "1" });
check("按上课日筛选(周一)", r.data.items.length > 0 && r.data.items.every((c) => c.schedule.some((s) => s.day === 1)), `${r.data.items.length} 门`);
r = await call("GET", "/api/courses", { teacher: "刘洋" });
check("按教师筛选", r.data.items.length === 3 && r.data.items.every((c) => c.teacher === "刘洋"), r.data.items.map((c) => c.name).join(","));

// ---------- 四类冲突检测 ----------
r = await call("GET", "/api/courses", {});
const cs201 = r.data.items.find((c) => c.id === 5); // 机器学习导论，先修 线性代数（a1 未修）
check("先修缺失软冲突", cs201.conflictDetail.some((x) => x.type === "prereq" && x.level === "soft"), cs201.conflictDetail.find((x) => x.type === "prereq")?.message);
const hasTime = r.data.items.some((c) => c.conflictDetail.some((x) => x.type === "time" && x.level === "hard"));
check("时间硬冲突可检出", hasTime);
r = await call("PUT", "/api/admin/rules", {}, { creditLimit: 12 }); // a1 已选 11 学分
r = await call("GET", "/api/courses", {});
const cs201b = r.data.items.find((c) => c.id === 5);
check("学分超限软冲突", cs201b.conflictDetail.some((x) => x.type === "credit" && x.level === "soft"), cs201b.conflictDetail.find((x) => x.type === "credit")?.message);
await call("PUT", "/api/admin/rules", {}, { creditLimit: 30 });

// ---------- 心愿单 + 四态票据 ----------
await call("POST", "/api/wishlist", {}, { courseId: 26 });
await call("POST", "/api/wishlist", {}, { courseId: 32 });
r = await call("POST", "/api/selection/submit", {}, {});
check("submit 202 受理", r.status === 202 && r.data.ticket.status === "已受理" && typeof r.data.ticket.queuePos === "number",
  `${r.data.ticket.id} 队列第 ${r.data.ticket.queuePos} 位`);
const tk = r.data.ticket.id;
await new Promise((s) => setTimeout(s, 1500));
r = await call("GET", "/api/selection/status/" + tk);
check("票据终态为四态之一", ["成功", "失败"].includes(r.data.status), `${r.data.status} 成功${r.data.accepted.length}/失败${r.data.rejected.length}`);

// ---------- 放号订阅 ----------
let full = db.allCourses().find((c) => c.remaining === 0);
if (!full) { db.takeSeat(db.allCourses()[0].id); }
full = db.allCourses().find((c) => c.remaining === 0) || db.allCourses()[0];
db.releaseSeat(full.id, 0); // 确保满员
r = await call("POST", "/api/subscribe", {}, { courseId: full.id });
check("订阅放号", r.status === 200, full.name);
const before = db.state.messages.length;
db.releaseSeat(full.id, 2); // 放号
const sent = svc.notifySubscribers(full);
check("放号后推送消息", sent >= 1 && db.state.messages.length > before && db.state.messages[0].type === "seat", db.state.messages[0]?.title);

// ---------- 课表导出 ICS ----------
r = await call("GET", "/api/timetable.ics");
check("ICS 导出", r.status === 200 && r.data.ics.startsWith("BEGIN:VCALENDAR") && r.data.ics.includes("SUMMARY"),
  `${r.data.ics.split("BEGIN:VEVENT").length - 1} 个事件`);

// ---------- 多账号隔离 ----------
r = await call("GET", "/api/me");
check("me 当前账号 a1", r.status === 200 && r.data.accountId === "a1" && r.data.accounts.length === 3,
  r.data.accounts.map((a) => a.name).join("/"));
const a1Enrolled = db.enrolled().length;
r = await call("POST", "/api/accounts/switch", {}, { accountId: "a2" });
check("切换到 a2", r.status === 200 && r.data.user.name === "苏同学");
check("账号状态隔离", db.enrolled().length !== a1Enrolled || db.state.wishlist.length === 0, `a1 已选 ${a1Enrolled} → a2 已选 ${db.enrolled().length}`);
r = await call("POST", "/api/accounts/switch", {}, { accountId: "a1" });
check("切回 a1", db.enrolled().length === a1Enrolled);

// ---------- 教师端 / 教务端回归 ----------
r = await call("GET", "/api/teacher/me");
check("教师档案", r.status === 200 && r.data.courseCount === 3, `${r.data.name} 授 ${r.data.courseCount} 门`);
r = await call("GET", "/api/teacher/courses/1/roster");
check("非本人课程 403", r.status === 403);
r = await call("GET", "/api/admin/anomalies", {});
check("异常工单", r.status === 200 && r.data.items.length === 3, `${r.data.items.length} 条`);
r = await call("GET", "/api/nope");
check("未知接口 404", r.status === 404);

console.log(fail === 0 ? "\n全部通过 ✅" : `\n有 ${fail} 项失败 ❌`);
process.exit(fail === 0 ? 0 : 1);
