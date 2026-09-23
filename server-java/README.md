# course-platform-server（Java 后端）

高校选课平台的 Java 后端工程（Spring Boot 3 + MyBatis-Plus + MySQL 8）。
本目录与原有 Node.js 实现（`../server`、`../docs/js/backend`）**并存**，用于承接「后端改为 Java + 本地 MySQL + 学生端/教师端密码登录」的改造目标。

## 1. 环境要求

| 组件 | 版本 |
|---|---|
| JDK | 25（`java.version=25`） |
| Maven | 3.9+ |
| MySQL | 8.0+（本地实例，默认端口 3306） |
| Spring Boot | 3.2.5 |
| MyBatis-Plus | 3.5.7（Spring Boot 3 专用 starter） |

## 2. 数据库初始化（三步）

```bash
# ① 建库
mysql -u root -p < src/main/resources/db/00-create-database.sql
# ② 建表
mysql -u root -p course_platform < src/main/resources/db/schema.sql
# ③ 灌入模拟数据
mysql -u root -p course_platform < src/main/resources/db/data.sql
```

脚本说明：

| 文件 | 作用 |
|---|---|
| `db/00-create-database.sql` | 创建 `course_platform` 库（utf8mb4）；可选创建应用专用账号（种子账号默认注释） |
| `db/schema.sql` | 16 张表的完整 DDL（含注释、索引、外键、派生列），执行前会 DROP 同名表 |
| `db/data.sql` | 模拟数据：40 门课程 + 排课、3 学生 / 38 教师 / 1 教务账号、选课记录、先修、规则、工单、消息等 |

> 也可将 `application.yml` 中 `spring.sql.init.mode` 改为 `always`，由 Spring Boot 启动时自动执行 `schema.sql` + `data.sql`（须先完成建库）。注意：`schema.sql` 含 `DROP TABLE`，反复启动会重置数据。

## 3. 数据源配置

编辑 `src/main/resources/application.yml`，或直接设置环境变量（推荐，避免把密码写进仓库）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/course_platform?... # 默认库 course_platform
    username: root
    password: ${MYSQL_PASSWORD:CHANGE_ME_LOCAL_MYSQL_PASSWORD}   # 占位项，需替换
```

PowerShell 设置环境变量示例（当前会话生效）：

```powershell
$env:MYSQL_PASSWORD = "你的本机MySQL密码"
```

## 4. 启动与验证

```bash
mvn spring-boot:run                 # 开发启动
mvn clean package                   # 打包（生成 target/course-platform-server-1.0.0.jar）
java -jar target/course-platform-server-1.0.0.jar
```

验证骨架是否就绪（无需数据库连接）：

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/health
# => { ok: true, service: "course-platform", serverTime: ..., uptime: ..., time: ... }
```

## 5. 初始登录账号（密码均为 BCrypt 哈希存储）

| 端 | 登录名 | 初始密码 | 说明 |
|---|---|---|---|
| 学生端 | `2023010101` / `2023010102` / `2023010103` | `123456` | 林同学（计科，已选 3 门）/ 苏同学（视传，已选 2 门）/ 何同学（数学，未选课） |
| 教师端 | `T1001` ~ `T1038` | `teacher123` | 工号即登录名；演示教师「刘洋」= `T1005`（3 门课） |
| 教务端（兼容保留） | `admin` | `admin123` | 对应现有教务页面，可后续按需启用 |

> 角色字段 `sys_user.role` 取值 `STUDENT` / `TEACHER` / `ADMIN`，多端登录时按角色路由到对应页面与接口权限。

## 6. 目录结构

```
server-java/
├── pom.xml                                   Maven 配置（JDK17 / SpringBoot3 / MyBatis-Plus / MySQL）
├── README.md
└── src/main/
    ├── java/com/courseplatform/
    │   ├── CoursePlatformApplication.java     启动类
    │   └── controller/HealthController.java   健康检查（GET /api/health）
    └── resources/
        ├── application.yml                    端口 8080、MySQL 数据源、MyBatis-Plus、业务参数
        └── db/
            ├── 00-create-database.sql         建库
            ├── schema.sql                     建表（16 张表）
            └── data.sql                       模拟数据
```

## 7. 当前进度与后续待补

已完成（本工程骨架 + 数据库层）：

- Maven 工程与依赖、启动类、`application.yml`（端口 8080、本地 MySQL 数据源、密码占位项）
- 数据库设计（16 张表，含账号表密码/角色字段）与脚本、40 门课程等模拟数据
- 数据库设计说明文档（见结果产物目录）

待补（后续任务）：

- 实体类 / Mapper / Service / Controller，实现原 Node 版 37 条 REST 接口（接口清单见 `../server/router.js`）
- 登录与鉴权：登录接口、Token 下发与校验、按角色的接口隔离（对应需求 I-09 会话隔离）
- 静态资源托管与前端联调（`../client/js/api.js` 为唯一请求出口，响应结构需保持兼容）
- SSE 名额推送（原 `GET /api/stream/seats`）与名额模拟任务
