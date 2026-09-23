/* 端到端测试：三端登录鉴权 + 选课全流程（对运行中的 http://127.0.0.1:8080，Java 后端）
 * 运行：node e2e.mjs（需先启动 java -jar server-java/target/course-platform-server-1.0.0.jar）
 * 数据基线：Java 种子（19 表）——3 学生 / 38 教师 / 40 门课；
 *   学生 2023010101（林同学）已选 5 门 / 心愿单 1 门（PE102 羽毛球，名额已满）；
 *   教师 T1005（刘洋）授课 3 门；教务 admin；选课规则学分上限 30；工单 3 条待处理。 */

const BASE = "http://127.0.0.1:8080";
let pass = 0, fail = 0;
function check(name, cond, extra = "") {
  if (cond) { pass++; console.log(`  ok  ${name}`); }
  else { fail++; console.log(`  FAIL ${name}  ${extra}`); }
}
async function req(method, path, body, token) {
  const headers = {};
  if (token) headers.Authorization = "Bearer " + token;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const res = await fetch(BASE + path, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
  const raw = await res.text();           // 先一次性读完，避免 body 被消费两次
  let data = null;
  try { data = JSON.parse(raw); } catch { /* 非 JSON（CSV/ICS） */ }
  return { status: res.status, data, text: raw };
}

/* ---- 1. 健康检查与鉴权门禁 ---- */
let r = await req("GET", "/api/health");
check("健康检查 ok=true", r.status === 200 && r.data.ok === true, JSON.stringify(r.data));

r = await req("GET", "/api/courses");
check("未登录访问受保护接口 → 401 + AUTH_REQUIRED", r.status === 401 && r.data.code === "AUTH_REQUIRED", JSON.stringify(r.data));

r = await req("GET", "/api/status");
check("公开端点 /api/status 含 openAt", r.status === 200 && typeof r.data.openAt === "number", JSON.stringify(r.data));

/* ---- 2. 三端登录 ---- */
r = await req("POST", "/api/auth/login", { username: "2023010101", password: "123456", role: "student" });
check("学生登录（2023010101/123456）", r.status === 200 && r.data.token && r.data.session.role === "STUDENT", JSON.stringify(r.data));
const stu = r.data.token;

r = await req("POST", "/api/auth/login", { username: "2023010101", password: "wrong", role: "student" });
check("学生错误密码 → 401", r.status === 401);

r = await req("POST", "/api/auth/login", { username: "T1005", password: "teacher123", role: "teacher" });
check("教师登录（T1005/teacher123）", r.status === 200 && r.data.token && r.data.session.role === "TEACHER", JSON.stringify(r.data));
const tea = r.data.token;

r = await req("POST", "/api/auth/login", { username: "admin", password: "admin123", role: "admin" });
check("教务登录（admin/admin123）", r.status === 200 && r.data.token && r.data.session.role === "ADMIN", JSON.stringify(r.data));
const adm = r.data.token;

r = await req("POST", "/api/auth/login", { username: "admin", password: "admin123", role: "STUDENT" });
check("教务账号选学生端 → 401（端与账号绑定）", r.status === 401);

r = await req("POST", "/api/auth/login", { username: "2023010101", password: "123456" });
check("缺少登录端 → 401", r.status === 401);

/* ---- 3. 学生端：课程 / 心愿单 / 选课受理 ---- */
r = await req("GET", "/api/courses", undefined, stu);
check("学生课程列表共 40 门", r.status === 200 && r.data.total === 40, `total=${r.data && r.data.total}`);
check("课程含已选（5 门）/心愿单标记", Array.isArray(r.data.items)
  && r.data.items.filter((c) => c.selected).length === 5
  && r.data.items.some((c) => c.inWishlist), `selected=${r.data.items && r.data.items.filter((c) => c.selected).length}`);

r = await req("GET", "/api/filters", undefined, stu);
check("筛选项枚举（类别/校区/教师），字段名与前端契约一致",
  r.status === 200 && Array.isArray(r.data.category) && Array.isArray(r.data.campus)
  && Array.isArray(r.data.teachers) && r.data.teachers.length > 0,
  JSON.stringify(r.data && { category: r.data.category, campus: r.data.campus, teachers: (r.data.teachers || []).length }));

r = await req("GET", "/api/wishlist", undefined, stu);
check("林同学心愿单 1 门（PE102）", r.status === 200 && r.data.items.length === 1
  && r.data.items[0].course.code === "PE102", JSON.stringify(r.data.items));

r = await req("POST", "/api/selection/submit", {}, stu);
check("提交选课 → 受理票据", (r.status === 202 || r.status === 200) && r.data.ticket, JSON.stringify(r.data));
const tid = r.data.ticket && r.data.ticket.id;
await new Promise((res) => setTimeout(res, 1800));
r = await req("GET", "/api/selection/status/" + tid, undefined, stu);
check("票据处理完成（成功+拒绝=总数，明确结论）", r.status === 200
  && (r.data.status === "成功" || r.data.status === "失败")
  && r.data.accepted.length + r.data.rejected.length === r.data.total, JSON.stringify(r.data));

r = await req("GET", "/api/timetable", undefined, stu);
check("课表含 5 门已选课程", r.status === 200 && r.data.courses.length === 5, `n=${r.data && r.data.courses && r.data.courses.length}`);

r = await req("GET", "/api/timetable.ics", undefined, stu);
check("课表 ICS 导出（text/calendar）", r.status === 200 && r.text.startsWith("BEGIN:VCALENDAR"), `status=${r.status}`);

r = await req("GET", "/api/messages", undefined, stu);
check("学生消息（欢迎 + 选课结果）", r.status === 200 && r.data.items.length >= 2, `n=${r.data && r.data.items && r.data.items.length}`);

/* ---- 4. 教师端 ---- */
r = await req("GET", "/api/teacher/courses", undefined, tea);
check("刘洋授课 3 门", r.status === 200 && r.data.items.length === 3, `n=${r.data && r.data.items && r.data.items.length}`);
const teaCourse = r.data.items[0];
r = await req("GET", `/api/teacher/courses/${teaCourse.id}/roster`, undefined, tea);
check("教师查看选课名单（真实学生）", r.status === 200 && Array.isArray(r.data.students) && r.data.students.length > 0, JSON.stringify(r.data.students && r.data.students.slice(0, 2)));

r = await req("GET", `/api/teacher/courses/${teaCourse.id}/roster.csv`, undefined, tea);
check("教师名单 CSV 导出（非空且非 JSON 报错）", r.status === 200 && !r.data && r.text.length > 100, `status=${r.status} len=${r.text && r.text.length}`);

r = await req("GET", "/api/teacher/courses", undefined, stu);
check("学生访问教师端 → 403", r.status === 403 && r.data.code === "FORBIDDEN", JSON.stringify(r.data));

/* ---- 5. 教务端 ---- */
r = await req("GET", "/api/admin/overview", undefined, adm);
check("教务总览（40 门课 / 工单 3 待处理）", r.status === 200 && r.data.courseCount === 40 && r.data.pendingAnomalies === 3, JSON.stringify(r.data && { cc: r.data.courseCount, pa: r.data.pendingAnomalies }));

r = await req("GET", "/api/admin/rules", undefined, adm);
check("教务读取规则（学分上限 30）", r.status === 200 && r.data.creditLimit === 30, JSON.stringify(r.data));

r = await req("PUT", "/api/admin/rules", { creditLimit: 32 }, adm);
check("教务修改规则 → 32", r.status === 200 && r.data.creditLimit === 32, JSON.stringify(r.data));
r = await req("PUT", "/api/admin/rules", { creditLimit: 30 }, adm);
check("教务改回规则 → 30", r.status === 200 && r.data.creditLimit === 30, JSON.stringify(r.data));

r = await req("GET", "/api/admin/anomalies", undefined, adm);
check("教务工单 3 条待处理", r.status === 200 && r.data.items.filter((a) => a.status === "pending" || a.status === "PENDING").length === 3, `n=${r.data && r.data.items && r.data.items.length}`);

r = await req("GET", "/api/admin/overview", undefined, tea);
check("教师访问教务端 → 403", r.status === 403);

/* ---- 6. 会话信息与退出 ---- */
r = await req("GET", "/api/me", undefined, stu);
check("/api/me 返回 session + profile（林同学）", r.status === 200 && r.data.session && r.data.profile && r.data.profile.name === "林同学", JSON.stringify(r.data.profile));

r = await req("POST", "/api/auth/logout", {}, stu);
check("退出登录", r.status === 200 && r.data.ok);
r = await req("GET", "/api/courses", undefined, stu);
check("退出后 token 失效（黑名单）→ 401", r.status === 401 && r.data.code === "AUTH_REQUIRED", JSON.stringify(r.data));

console.log(`\n结果：${pass} 通过，${fail} 失败`);
process.exit(fail ? 1 : 0);
