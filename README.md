---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: e6d45bac59d21b2ad19738d6bf675356_a38c1bf7b6ef11f18db252540024e231
    ReservedCode1: U9YrIsci8SAyptRTgMYAwHC2/4JeXJ6ypaHOWuGcIK8WSgtMWXC1lZ0TvtCToS2nAV6oSb4RVfELMWkt+3OhckOuCnXosUoMWzLrfsODt0ruDt/X4ttBUHHfkcCMhZVZu5WmdXauAEoJCaoVp7gAtRLb7Cc9kr5FO0kk6T0u9hXylCjEo6mp8qowFIA=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: e6d45bac59d21b2ad19738d6bf675356_a38c1bf7b6ef11f18db252540024e231
    ReservedCode2: U9YrIsci8SAyptRTgMYAwHC2/4JeXJ6ypaHOWuGcIK8WSgtMWXC1lZ0TvtCToS2nAV6oSb4RVfELMWkt+3OhckOuCnXosUoMWzLrfsODt0ruDt/X4ttBUHHfkcCMhZVZu5WmdXauAEoJCaoVp7gAtRLb7Cc9kr5FO0kk6T0u9hXylCjEo6mp8qowFIA=
---

# 高校选课平台（Course Selection Platform）

基于《高校选课系统设计方案》实现的前后端分离原型。**多端登录（学生端 / 教师端 / 教务端），
账号密码持久化于本地 MySQL，后端为 Java（Spring Boot 3）。**
前端原生 ES Module（无框架、无构建步骤），由后端一并托管，访问 8080 即打开登录页。

- 后端：`server-java/`（Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 + JWT + MySQL 8，唯一后端）
- 前端：`client/`（原生 ESM）
- 数据库：`course_platform`（16 张表，脚本 `reset-db-java.mjs`）

## 运行

```bash
# 依赖：JDK 17+、Maven 3.9+、本地 MySQL 8（默认 root/root）
cd server-java
mvn clean package -DskipTests
java -Dserver.port=8080 -jar target/course-platform-server-1.0.0.jar
# 打开 http://127.0.0.1:8080
```

首次需初始化数据库（建库 → 建表 → 灌演示数据）：

```bash
node reset-db-java.mjs        # 依次执行 db/00-create-database.sql → schema.sql → data.sql
```

MySQL 密码非默认值时用环境变量覆盖：`MYSQL_PASSWORD=你的密码 java -jar ...`。

## 演示账号（三端独立登录，登录时需选择端）

| 端 | 账号 | 密码 | 说明 |
|---|---|---|---|
| 学生端 | `2023010101`（林同学） | `123456` | 已预置 5 门课、1 门心愿单（PE102 羽毛球） |
| 学生端 | `2023010102` / `2023010103` | `123456` | 苏同学（已选 2 门）/ 何同学（未选课） |
| 教师端 | `T1005`（刘洋） | `teacher123` | 授课 3 门；其余教师 T1001~T1038 |
| 教务端 | `admin` | `admin123` | 全局管理：规则、工单、容量、监控、数据重置 |

> `sys_user.role` 取值为 `STUDENT` / `TEACHER` / `ADMIN`；账号与端绑定，选错端返回 401。

## 目录结构

```
course-platform/
├── e2e.mjs                        # 端到端测试（31 项断言，需服务已启动）
├── reset-db-java.mjs              # 数据库重建工具（16 表 + 演示数据）
├── server-java/                   # —— 后端（Spring Boot 3）——
│   ├── pom.xml                    #   Maven：SpringBoot3 / MyBatis-Plus / jjwt / MySQL Connector
│   └── src/main/
│       ├── java/com/courseplatform/
│       │   ├── CoursePlatformApplication.java  启动类（@EnableScheduling）
│       │   ├── config/            WebConfig（拦截器注册）/ AuthInterceptor（鉴权 + 按端隔离）
│       │   │                      MetricsRegistry / ServerClock
│       │   ├── security/          JwtUtil（HS256 签发校验）/ LoginUser / AuthContext
│       │   │                      TokenBlacklist（登出令牌作废）
│       │   ├── controller/        Auth / Me / Course / Student / Teacher / Admin / Meta / Health / Stream
│       │   ├── service/           AuthService / CourseService / StudentService / TeacherService / AdminService
│       │   ├── mapper/            MyBatis-Plus Mapper
│       │   └── entity/            实体类
│       └── resources/
│           ├── application.yml    端口 8080、MySQL 数据源、MyBatis-Plus、业务参数
│           └── db/                00-create-database.sql / schema.sql / data.sql / reset-demo-data.sql
└── client/                        # —— 前端（原生 ESM，无框架）——
    ├── index.html                 页面骨架 + 三端登录页
    ├── css/main.css               设计令牌 + 布局 + 组件 + 登录页样式
    └── js/                        api / store / ui / views / main
```

## 登录与鉴权模型

- `POST /api/auth/login` 提交 `{username, password, role}`，`role` 取 `student|teacher|admin`（不区分大小写）。
  密码以 BCrypt 哈希存于 `sys_user`，校验通过返回 **JWT（HS256，2 小时有效）**。
- 前端经 `Authorization: Bearer <token>` 携带；SSE 与文件下载（课表 `.ics`、名单 `.csv`）
  由浏览器直接发起、无法附加请求头，故统一支持 `?token=` 查询参数。
- 登录成功返回 `token` + `session`（含 userId / role / realName，学生附 studentNo/grade/major，
  教师附 teacherNo/title/dept）；`GET /api/me` 另返回 `profile` 与 `accounts`，供设置页展示当前身份。
- 接口按端隔离：`/api/teacher/*` 仅教师、`/api/admin/*` 与 `/api/architecture` 仅教务，
  越权返回 403 + `code: "FORBIDDEN"`；未登录返回 401 + `code: "AUTH_REQUIRED"`，前端自动跳回登录页。
- **登出即失效**：`POST /api/auth/logout` 会把当前 token 写入内存黑名单（条目随其自然过期自动清理），
  此后该 token 请求一律 401 —— 弥补 JWT 无状态无法主动作废的缺口。

## 数据库表（course_platform，16 张）

定义见 `server-java/src/main/resources/db/schema.sql`，演示数据见 `data.sql`。

账号与档案：`sys_user`（三端账号，BCrypt 密码 + `role`）、`student`、`teacher`；
教学：`course`（含虚拟生成列 `remaining = capacity - enrolled`）、`course_schedule`（排课）、
`course_prerequisite`（先修关系）、`student_course`（选课记录）；
选课流程：`wishlist`（心愿单 / 志愿梯队）、`seat_subscription`（放号订阅）、
`selection_ticket`（受理票据）、`selection_ticket_item`（票据明细，逐门结果）；
消息与配置：`message`（按 `user_id` 隔离）、`user_preference`（偏好设置）、
`selection_rule`（教务规则）、`selection_period`（选课阶段：预选 / 正选 / 补退选）、
`anomaly_ticket`（异常工单）。

名额 `course.enrolled` 由条件 UPDATE 原子扣减，`remaining` 由 MySQL 虚拟生成列实时推导，杜绝超选。

## 端到端测试

```bash
node e2e.mjs   # 31 项断言：健康检查 / 三端登录 / 端账号绑定 / 错误密码 /
               # 401 门禁 / 未授权 403 / 课程与筛选 / 心愿单 / 选课受理票据 /
               # 课表与 ICS 导出 / 名单与 CSV / 教务规则读写 / 工单 / 登出令牌失效
```

## 功能（对应设计方案）

| 功能 | 设计依据 | 实现位置 |
|---|---|---|
| 多端独立登录 | 三端账号体系 | 登录页 + AuthController/AuthService |
| 多维筛选 + 「仅看不冲突」 | 查找筛选耗时 71% | 课程中心 |
| 时间冲突 / 跨校区通勤冲突预检测 | 冲突检测需求 74% | 课程卡片 / 心愿单预警 |
| 心愿单 + 志愿梯队 + 一键批量提交 | 削峰 + 「两三个方案」诉求 | 心愿单 |
| 受理回执 + 异步处理 + 结果推送 | 提交无响应 29% | 提交 → 轮询票据 → 结果弹窗 |
| 抢课专区：倒计时 + 实时名额 | 刷新难找 61.54% | 抢课专区（SSE `GET /api/stream/seats`） |
| 课表周视图 / 列表视图 + ICS 导出 | 偏好分裂 42.31% : 42.31% | 我的课表 |
| 教师工作台：名册 / 课程维护 / CSV 导出 | 教师端需求 | 教师端三页 |
| 教务管理：规则 / 工单 / 容量 / 监控 / 数据重置 | 教务端需求 | 教务端五页 |
| 深色模式 / 四档断点 / 触控热区 ≥44px | 移动端待优化 80.77% | main.css |
| 选课节点卡片（阶段名称 / 起止 / 倒计时，最后 1 小时高亮） | P-01-1 | 首页（`GET /api/status` → `phases`，读 `selection_period`） |
| 阶段驱动的倒计时（未开放时倒数到阶段开始，开放中显示进行中） | P-01-1 | `docs/js/phases.js`（两端共用） |
| 教务可编辑选课阶段时间，改完即时改变学生端倒计时 | P-01-1 | 教务端「规则配置」（`PUT /api/admin/periods/:code`） |
| 阶段窗口外的提交拦截（服务端强制 + 提示下一阶段） | P-01-1 | 两端 `submitSelection` / `StudentService` |

## 数据说明

`GET /api/status` 另返回 `phases`（来自 `selection_period` 表）与 `selectionOpen`，
驱动首页「选课节点」卡片与倒计时；该表未初始化时 `phases` 返回空数组、
卡片与倒计时自动隐藏，接口不因缺表报错。

**倒计时锚定在选课时间上，不随页面加载启动。** 前端消费 `phases` 的优先级为：

1. 有进行中的阶段 → 倒计时归零，展示「<阶段名>进行中」；
2. 否则取最近一个未开始阶段的开始时间 → 展示「距「<阶段名>」开放」+ 真实剩余时间；
3. 阶段表为空 → 回退后端 `openAt`；
4. 都没有 → 返回 `null`，**不显示倒计时**（而不是显示一个假数字）。

两端共用同一套口径：静态版 `selectionOpenAt()`、Java 版 `PeriodService.openAt()`，
以及前端的 `docs/js/phases.js`。教务在「规则配置 → 选课阶段时间」里改动起止时间后，
学生端倒计时下一次轮询即跟随变化，无需重启服务。

提交选课还有**服务端阶段门禁**：不在任何阶段窗口内时直接拒绝，并在提示语里点出
下一个阶段名与开放时间（如「当前不在选课阶段内，「正选」将于 9 月 23 日 12:52 开放」）。

服务启动后 `StreamController` 每 4 秒随机消耗若干名额并通过 SSE 推送，
模拟高峰期余量变化。教务端提供「重置演示数据」：清空课程活动数据并重新灌入
（账号与规则保留，选课阶段时间恢复种子值）。

## 后端架构说明

| 层 | 职责 |
|---|---|
| `controller` | REST 路由；`AuthContext.current(req)` 取当前登录用户，鉴权由拦截器统一完成 |
| `service` | 业务逻辑：筛选 / 冲突检测 / 心愿单 / 选课受理 / 教务规则 |
| `mapper` | MyBatis-Plus 数据访问 |
| `config.AuthInterceptor` | 令牌解析（请求头 / `?token=`）、黑名单校验、按路径角色隔离、运行监控埋点 |
| `exception.GlobalExceptionHandler` | 统一错误结构 `{error, code}`（与前端 `api.js` 契约一致） |
*（内容由AI生成，仅供参考）*
