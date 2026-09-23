/* 用 Java 版自带的 SQL 脚本重建 course_platform 库（16 表结构 + 模拟数据）
 * 运行：node reset-db-java.mjs
 * 说明：00 建库脚本不依赖库名；schema/data 需在 course_platform 库内执行（原设计走 mysql CLI 传库名）。 */
import fs from "node:fs";
import mysql from "mysql2/promise";

const ROOT = { host: "127.0.0.1", port: 3306, user: "root", password: "root", multipleStatements: true, charset: "utf8mb4" };
const LIB = { ...ROOT, database: "course_platform" };

async function run(conn, file) {
  const sql = fs.readFileSync(file, "utf8");
  process.stdout.write(`executing ${file} (${sql.length} chars) ... `);
  await conn.query(sql);
  console.log("ok");
}

// 阶段 1：建库（无库名连接）
let conn = await mysql.createConnection(ROOT);
await run(conn, "server-java/src/main/resources/db/00-create-database.sql");
await conn.end();

// 阶段 2：建表 + 灌数据（course_platform 库内）
conn = await mysql.createConnection(LIB);
await run(conn, "server-java/src/main/resources/db/schema.sql");
await run(conn, "server-java/src/main/resources/db/data.sql");

const [tables] = await conn.query("SHOW TABLES");
console.log("tables:", tables.map((r) => Object.values(r)[0]).join(", "));

for (const t of ["sys_user", "teacher", "student", "course", "course_schedule", "student_course", "wishlist"]) {
  try {
    const [rows] = await conn.query(`SELECT COUNT(*) AS n FROM ${t}`);
    console.log(`${t}: ${rows[0].n} rows`);
  } catch (e) { console.log(`${t}: ERR ${e.message}`); }
}

await conn.end();
console.log("done");
