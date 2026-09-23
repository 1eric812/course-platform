---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: e6d45bac59d21b2ad19738d6bf675356_a2515f20b6ef11f1b0b2525400638852
    ReservedCode1: skXkUza+Zm/3HCp/5rYjzECxLGCNCczofGoTJP3lYpnn0JxpKfxMiosXYz0R8zmifiICHzRVWcQD8LpPnIW1hHTKtOg7Si9C/PU9xzOoJe0pD/dkyJUtw6tmFBPxLEYFabTTI0Hyemxc8q4ou3W8XL5gVQ0HXYzZOxBsti+8WV89Zp0bjYLOX+mnHbQ=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: e6d45bac59d21b2ad19738d6bf675356_a2515f20b6ef11f1b0b2525400638852
    ReservedCode2: skXkUza+Zm/3HCp/5rYjzECxLGCNCczofGoTJP3lYpnn0JxpKfxMiosXYz0R8zmifiICHzRVWcQD8LpPnIW1hHTKtOg7Si9C/PU9xzOoJe0pD/dkyJUtw6tmFBPxLEYFabTTI0Hyemxc8q4ou3W8XL5gVQ0HXYzZOxBsti+8WV89Zp0bjYLOX+mnHbQ=
---

# course-platform-server（Java 后端）

高校选课平台的 Java 后端工程（Spring Boot 3 + MyBatis-Plus + MySQL 8），是当前**唯一后端实现**：
前端 `../client`（原生 ES Module）由本服务一并托管，访问 `http://127.0.0.1:8080` 即打开登录页。

## 1. 环境要求

| 组件 | 版本 |
|---|---|
| JDK | 17（`pom.xml` 中 `java.version=17`） |
| Maven | 3.9+ |
| MySQL | 8.0+（本地实例，默认 127.0.0.1:3306） |
| Spring Boot | 3.2.5 |
| MyBatis-Plus | 3.5.7（Spring Boot 3 专用 starter） |
| jjwt | 0.12.6（HS256） |

## 2. 数据库（`course_platform`，16 张表）

结构与数据脚本位于 `src/main/resources/db/`，以 `schema.sql` 与 `data.sql` 为准（**不可照搬旧 Node 版 SQL**）。

```bash
# 推荐：在 course-platform 根目录执行（建库 → 建表 → 灌演示数据 → 打印表清单与关键行数）
node reset-db-java.mjs
```

等价的手动三步：

```bash
mysql -u root -p < src/main/resources/db/00-create-database.sql
mysql -u root -p course_platform < src/main/resources/db/schema.sql
mysql -u root -p course_platform < src/main/resources/db/data.sql
```

| 分组 | 表 |
|---|---|
| 账号与档案 | `sys_user`（三端账号，BCrypt 密码 + `role`）、`student`、`teacher` |
| 教学 | `course`（虚拟生成列 `remaining = capacity - enrolled`）、`course_schedule`、`course_prerequisite`、`student_course` |
| 选课流程 | `wishlist`、`seat_subscription`、`selection_ticket`、`selection_ticket_item` |
| 消息与配置 | `message`（按 `user_id` 隔离）、`user_preference`、`selection_rule`、`selection_period`、`anomaly_ticket` |

> `schema.sql` 内含 `DROP TABLE`，重复执行会重置数据；`application.yml` 中 `spring.sql.init.mode` 默认为 `never`，启动时不会自动执行脚本。

## 3. 数据源与密钥

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/course_platform?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: ${MYSQL_PASSWORD:root}          # 非默认密码用环境变量覆盖
course-platform:
  auth:
    token-ttl-seconds: 7200
    jwt-secret: ${JWT_SECRET:course-platform-demo-secret-key-please-change-in-prod-2026}
```

```powershell
$env:MYSQL_PASSWORD = "你的本机 MySQL 密码"   # 仅当前会话生效
```

## 4. 启动与验证

```bash
mvn spring-boot:run                                  # 开发启动
mvn clean package -DskipTests                        # 打包
java -jar target/course-platform-server-1.0.0.jar     # 运行（默认 8080）
```

端口被占用时可覆盖：`-Dserver.port=8082` 或环境变量 `SERVER_PORT=8082`。

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/health   # {ok, service, db, serverTime, uptime}
Invoke-RestMethod http://127.0.0.1:8080/api/status   # 含 phases（选课阶段），未初始化 stage 表时为空数组
```

## 5. 登录账号（密码为 BCrypt 哈希）

| 端 | 登录名 | 密码 | 说明 |
|---|---|---|---|
| 学生端 | `2023010101` / `2023010102` / `2023010103` | `123456` | 林同学 / 苏同学 / 何同学 |
| 教师端 | `T1005`（其余 `T1001`~`T1038`） | `teacher123` | 刘洋，授课 3 门 |
| 教务端 | `admin` | `admin123` | 规则、工单、容量、监控、数据重置 |

`sys_user.role` 取 `STUDENT` / `TEACHER` / `ADMIN`；账号与端绑定，选错端返回 401。

## 6. 接口清单（按 Controller）

| Controller | 接口 |
|---|---|
| `AuthController` | `POST /api/auth/login`、`POST /api/auth/logout` |
| `MeController` | `GET /api/me`（含 `session` / `profile` / `accounts`） |
| `HealthController` | `GET /api/health` |
| `MetaController` | `GET /api/status`（含 `phases`）、`GET /api/architecture` |
| `CourseController` | `GET /api/filters`、`GET /api/courses`、`GET /api/courses/{id}` |
| `StudentController` | 心愿单 `GET/POST /api/wishlist`、`DELETE /api/wishlist/{id}`、`PUT /api/wishlist/reorder`；受理 `POST /api/selection/submit`、`GET /api/selection/status/{id}`；课表 `GET /api/timetable`、`GET /api/timetable.ics`；消息 `GET /api/messages`、`POST /api/messages/read`；偏好 `GET/PUT /api/preferences`；订阅 `GET /api/subscriptions`、`POST /api/subscribe`、`POST /api/unsubscribe` |
| `TeacherController` | `GET /api/teacher/me`、`GET /api/teacher/courses`、`GET /api/teacher/courses/{id}/roster`、`GET /api/teacher/courses/{id}/roster.csv`、`PUT /api/teacher/courses/{id}` |
| `AdminController` | `GET /api/admin/overview`、`GET/PUT /api/admin/rules`、`GET /api/admin/monitor`、`GET /api/admin/anomalies`、`POST /api/admin/anomalies/{id}/resolve`、`POST /api/admin/courses/{id}/seats`、`POST /api/admin/reset` |
| `StreamController` | `GET /api/stream/seats`（SSE 名额推送） |

## 7. 鉴权与多端隔离

- 登录成功返回 JWT（HS256，默认 2 小时）+ `session`；请求经 `Authorization: Bearer <token>` 携带；
  SSE 与文件下载（`.ics` / `.csv`）无法附加请求头，统一支持 `?token=`。
- `config.AuthInterceptor` 统一完成令牌解析、黑名单校验与按路径的端隔离：
  `/api/teacher/*` 仅教师、`/api/admin/*` 与 `/api/architecture` 仅教务；越权 403，未登录 401。
- `POST /api/auth/logout` 将当前 token 写入内存黑名单（`security.TokenBlacklist`），登出即失效。
- 统一错误结构 `{error, code}`（`exception.GlobalExceptionHandler`），与前端 `api.js` 契约一致。

## 8. 选课阶段（`selection_period`）

`GET /api/status` 额外返回 `phases`，驱动首页「选课节点」卡片（P-01-1）：

```json
{"code":"PRE","name":"预选","start":"2026-09-18T09:00:00","end":"2026-09-20T23:59:59",
 "status":"FINISHED","current":false,"remark":"仅可加入心愿单，不做名额占用"}
```

阶段按 `start_time` 升序返回；`current` 由服务端按当前时间动态判定；表为空或查询异常时返回空数组，接口不报错，前端自动隐藏该卡片。

## 9. 目录结构

```
server-java/
├── pom.xml
└── src/main/
    ├── java/com/courseplatform/
    │   ├── CoursePlatformApplication.java      启动类（@EnableScheduling）
    │   ├── config/         WebConfig / AuthInterceptor / MetricsRegistry / ServerClock
    │   ├── security/       JwtUtil / LoginUser / AuthContext / TokenBlacklist
    │   ├── controller/     Auth / Me / Course / Student / Teacher / Admin / Meta / Health / Stream
    │   ├── service/        AuthService / CourseService / StudentService / TeacherService / AdminService / WishlistService
    │   ├── mapper/         MyBatis-Plus Mapper（含 CoreMapper 自定义 SQL）
    │   ├── entity/         16 张表对应实体（含 SelectionPeriod）
    │   └── exception/      GlobalExceptionHandler
    └── resources/
        ├── application.yml 端口 8080、MySQL 数据源、MyBatis-Plus、业务参数
        └── db/             00-create-database.sql / schema.sql / data.sql / reset-demo-data.sql
```

## 10. 现状与遗留

已实现：16 张表结构与数据脚本、9 个 Controller 全部接口、多端密码登录（JWT + 端隔离 + 登出黑名单）、
多维筛选与「仅看不冲突」、四类冲突检测（时间 / 跨校区 / 先修 / 学分上限）、心愿单与志愿梯队、
受理票据四态、SSE 名额推送与降级、课表与 `.ics` / `.csv` 导出、教师工作台、教务规则 / 工单 / 容量 / 监控 / 重置。

遗留：

1. 本机 8081 端口驻留一个**旧版** Java 实例（启动时间早于本次改造），并锁定 `target/course-platform-server-1.0.0.jar`，
   导致 `mvn clean package` 无法重命名产物而失败；如需重新打包，请先结束该进程（或改用 `mvn spring-boot:run`）。
2. `selection_period` 目前仅有只读接口（`GET /api/status.phases`），教务端缺少阶段维护接口（新增 / 修改 / 状态流转）。
3. 首页「选课节点」卡片仅完成语法与接口层验证，尚未做浏览器端多断点 UI 回归。
*（内容由AI生成，仅供参考）*
