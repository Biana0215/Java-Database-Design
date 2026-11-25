# 软件系统综合设计 · 课程设计工程

Java + openGauss 的小型信息管理系统课设（4 人小组，2 周）。

## 目录约定

```
Java_Database_Design/
├─ lib/                 JDBC 驱动（不进 Git）
├─ sql/                 01_schema.sql / 02_data.sql / 99_query.sql
├─ docs/                需求分析、数据库设计说明书、课程设计报告
├─ tools/环境验证/      DbCheck.java：连库与环境自检
└─ src/                 （待建）Java 源码
```

## 数据库环境（2026-11-25 实测）

| 项 | 值 |
|---|---|
| 服务端 | openGauss / GaussDB Kernel V500R002C00（`server_version` 报 9.2.4） |
| 地址 | `192.168.98.129:7654` |
| 业务库 | **`hospital`**（已建，`ENCODING=UTF8`，`LC_COLLATE=C`，`DBCOMPATIBILITY=A`） |
| 连接用户 | `test_user`（`super=false`，已授 `CREATEDB`，`public` 下可建表） |
| 驱动 | `lib/opengauss-jdbc-5.1.0-og.jar`，本机 JDK 26 实测可用 |

连接串（驱动类 `org.opengauss.Driver`）：

```java
String url = "jdbc:opengauss://192.168.98.129:7654/hospital";
```

密码写在 `tools/环境验证/db-opengauss.properties`，该文件已被 `.gitignore` 排除。

### 已实测通过

`tools/环境验证/DbCheck.java` 七项全 `[OK]`：连接、`version()`、建表、事务回滚、事务提交、中文写入回读、主键唯一约束（重复插入 `SQLState=23505`）。

## 三条必须遵守的建表规则

### 1. `VARCHAR(n)` 的 n 是**字节**，不是字符

库是 `DBCOMPATIBILITY=A`（Oracle 兼容模式），实测：

| 写法 | 结果 |
|---|---|
| `VARCHAR(20)` 存 6 个汉字（18 字节） | 成功 |
| `VARCHAR(20)` 存 7 个汉字（21 字节） | 报 `22001` |
| `VARCHAR2(5 CHAR)` | 语法错 `42601`（不支持字符语义写法） |
| `NVARCHAR2(5)` 存 5 个汉字 | 成功（`NVARCHAR2` 按字符计） |

**结论：1 个汉字占 3 字节，`VARCHAR(n)` 只能存 n/3 个汉字。** 统一按下面的宽度取：

| 用途 | 类型 |
|---|---|
| 编号、编码、状态码 | `VARCHAR(20)` |
| 姓名、科室名、职称、药名 | `VARCHAR(60)`（20 个汉字） |
| 标题、地址、备注 | `VARCHAR(200)` 或 `TEXT` |
| 金额 | `NUMERIC(10,2)` |
| 日期 / 时间 | `DATE` / `TIMESTAMP` |

> 这条可以写进报告的“开发环境说明”：受数据库兼容模式影响，字符型字段长度按字节计，故中文列按 3 倍预留。

### 2. 内核是 PG 9.2，这些写法会直接语法报错

| 写法 | 结果 | 替代 |
|---|---|---|
| `SERIAL` 自增 | 可用 | —— |
| `GENERATED ALWAYS AS IDENTITY` | `42601` | 用 `SERIAL` |
| `ON CONFLICT (id) DO ...` | `42601` | `ON DUPLICATE KEY UPDATE`（本机可用） |
| `count(*) FILTER (WHERE ...)` | `42601` | `SUM(CASE WHEN ... THEN 1 ELSE 0 END)` |
| `LATERAL` 连接 | `42601` | 改子查询 / 相关子查询 |
| 窗口函数 `row_number() over()` | 可用 | —— |
| `WITH ... INSERT` | 可用 | —— |
| `string_agg` | 可用 | —— |
| `LIMIT / OFFSET` | 可用 | —— |
| `DATE - DATE` | 可用，得整数天 | 排班、区间天数直接用 |
| `CURRENT_DATE + 1` | 可用 | —— |

### 3. 命名与结构

- 表名、列名一律**小写下划线**（`reg_order`、`schedule_slot`），不加双引号（openGauss 会把未加引号的标识符折成小写）。
- 表建在 `hospital` 库的 `public` 模式下，不跨库（openGauss 不支持跨库查询）。
- 主键用 `SERIAL`；时间列用 `DATE`/`TIMESTAMP`；状态列用 `VARCHAR(20)` + `CHECK` 约束。

## 下一步

题目已定：**医院挂号系统**（8 张表 / 22 条功能，见 `docs/`）。

1. 需求一页纸找老师做可行性确认 → `docs/01_需求一页纸.md`
2. 按 `docs/03_分工表.md` 落地分工，第一天约定 DAO 接口签名
3. 设计：ER 图 → 关系模式 → 3NF 分析 → `sql/01_schema.sql`（由 A 负责，一次执行成功）
4. 编码：先纵向打通登录→挂号主流程，再横向铺开；每天合并

驱动 jar 随仓库分发（900KB，队友 clone 即用）。密码不进版本库：各人复制
`tools/环境验证/db-opengauss.properties.template` 为 `db-opengauss.properties` 再填自己的密码。
