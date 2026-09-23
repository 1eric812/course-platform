# 部署指南（Deployment Guide）

高校选课平台是**单服务架构**：前端（原生 ESM 静态文件）由 Spring Boot 后端一并托管，
访问后端端口即打开页面。所以部署只需要跑**一个 Java 服务 + 一个 MySQL**。

> 与「请假系统」不同，本项目**不能**部署到 GitHub Pages —— 它是 Java 常驻服务，
> 且依赖 MySQL 和 JWT 会话，GitHub Pages 只能托管纯静态资源。

---

## 一、最快方式：Docker Compose（推荐）

已提供完整编排文件，一条命令拉起 MySQL + 后端，首次自动建库建表灌数据。

```bash
cd course-platform

# 1. 准备环境变量
cp .env.docker.example .env
#    编辑 .env，至少把 MYSQL_ROOT_PASSWORD 和 JWT_SECRET 改掉

# 2. 启动
docker compose up -d --build

# 3. 访问
#    http://localhost:8080
```

演示账号（详见 README）：

| 端 | 账号 | 密码 |
|---|---|---|
| 学生端 | `2023010101` | `123456` |
| 教师端 | `T1005` | `teacher123` |
| 教务端 | `admin` | `admin123` |

常用运维命令：

```bash
docker compose logs -f backend    # 看后端日志
docker compose ps                 # 查看容器状态
docker compose down               # 停止（保留数据库数据）
docker compose down -v            # 停止并清空数据库（下次启动重新初始化）
```

---

## 二、部署到云平台

支持 Docker 的免费/低价平台都可以（Render、Railway、Fly.io、或自己的云服务器）。

### 步骤

1. **准备数据库**：在平台上新建 MySQL 8 实例，记下 host / port / user / password / database
2. **部署后端**：关联本仓库，平台会自动读取根目录 `Dockerfile`
3. **配置环境变量**：

   | 变量 | 必填 | 示例 |
   |---|---|---|
   | `DB_URL` | 是 | `jdbc:mysql://<host>:<port>/course_platform?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true` |
   | `DB_USERNAME` | 是 | `root` |
   | `DB_PASSWORD` | 是 | 你的数据库密码 |
   | `JWT_SECRET` | 是 | 随机字符串，**≥ 32 字节** |
   | `SERVER_PORT` | 否 | `8080`（平台一般自动注入 `PORT`） |

4. **初始化数据库**：首次部署后，把 `server-java/src/main/resources/db/` 下的
   `schema.sql` 和 `data.sql` 依次导入目标数据库：

   ```bash
   mysql -h <host> -u <user> -p < schema.sql
   mysql -h <host> -u <user> -p < database_name > data.sql
   ```

   > `data.sql` 里含中文演示数据，导入时建议加 `--default-character-set=utf8mb4`。

---

## 三、不用 Docker 的常规部署

若目标机器已有 JDK 17 和 MySQL 8：

```bash
# 1. 初始化数据库
mysql -u root -p < server-java/src/main/resources/db/00-create-database.sql
mysql -u root -p course_platform < server-java/src/main/resources/db/schema.sql
mysql -u root -p course_platform < server-java/src/main/resources/db/data.sql

# 2. 打包
cd server-java
mvn clean package -DskipTests

# 3. 启动（用环境变量覆盖数据库配置）
export DB_USERNAME=root
export DB_PASSWORD=你的密码
export JWT_SECRET=至少32字节的随机字符串
java -jar target/course-platform-server-1.0.0.jar
```

---

## 四、环境变量速查

后端全部配置项均可用环境变量覆盖（默认值见 `server-java/src/main/resources/application.yml`）：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `SERVER_PORT` | `8080` | 服务端口 |
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/course_platform?...` | 数据库连接串 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | `root`（兼容旧的 `MYSQL_PASSWORD`） | 数据库密码 |
| `JWT_SECRET` | 内置演示密钥 | JWT 签名密钥，生产必须更换 |
| `CLIENT_DIR` | `../client/` | 前端静态资源目录 |

---

## 五、常见问题

**Q：页面打开是白屏 / 404**
`CLIENT_DIR` 指向的前端目录不存在。Docker 镜像里已把 `client/` 放到 `/client`，
工作目录 `/app` 时 `../client` 正好指向它。非 Docker 部署请确认
`server-java` 的**上一级**存在 `client/` 目录。

**Q：后端启动报 `Communications link failure`**
`DB_URL` 主机名写错。Docker Compose 内必须用服务名 `mysql`，不能写 `127.0.0.1`
（那是容器自己，不是数据库容器）。

**Q：登录返回 401，账号密码没错**
检查是否选对了「端」。账号与端强绑定（学生 / 教师 / 教务），选错端会返回 401。

**Q：改名 / 换域名后登录提示 token 无效**
`JWT_SECRET` 变了会导致已签发的 token 全部失效，重新登录即可。

**Q：数据库中文乱码**
确认 MySQL 字符集为 `utf8mb4`；连接串带 `characterEncoding=UTF-8`（已内置）。

**Q：首次导入 `data.sql` 报外键错误**
必须按 `schema.sql` → `data.sql` 的顺序执行，后者依赖前者的表结构。
