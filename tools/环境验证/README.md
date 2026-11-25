# 环境验证：先用 10 分钟把 JDBC 跑通，再谈选题

> **2026-11-25 已在本机实测通过**：`openGauss @ 192.168.98.129:7654`（用户 `test_user`）+ `lib/opengauss-jdbc-5.1.0-og.jar` + JDK 26，
> 7 项检查全部 `[OK]`（连接、查询、建表、事务回滚、提交、中文、主键约束）。
> 结论：JDBC 这条链路不用再担心，直接进入建 schema 和写需求。

课设期间最贵的时间不是写代码，是环境卡住。这个目录就是用来把「Java 能不能连上你 VM 里的库」这件事，
在正式开始前一次性验死。

## 一、这个自检做了什么

`DbCheck.java` 会对目标库做 7 项检查，每项独立打印 `[OK]` / `[FAIL]`：

| 序号 | 检查项 | 为什么课设必须过 |
|---|---|---|
| 1 | 驱动类加载 | 驱动 jar 没加对，后面全是空谈 |
| 2 | 建立连接 | 网络模式、端口、监听地址、密码 |
| 3 | 执行查询 | 能读结果集，中文注释/SQL 没编码问题 |
| 4 | 建表 / 清空 | 你有 DDL 权限（课设要自己建库建表） |
| 5 | 事务回滚 | 后面「订房/号源冲突」要靠 `setAutoCommit(false)` |
| 6 | 事务提交 + 中文 | 字符集不是 UTF-8 的话，中文会变问号 |
| 7 | 主键唯一约束 | 冲突检测的正确做法是让数据库拦，不是先查后插 |

跑完会自动 `DROP TABLE env_check`，不会留垃圾表，也不会碰你已有的 `student` / `sc` 等表。

## 二、跑起来

### 1. 下载驱动

- **openGauss**：到 openGauss 官网下载 `opengauss-jdbc-x.x.x.jar`；或用 Maven 依赖 `org.opengauss:opengauss-jdbc`。
- **MySQL**：`mysql-connector-j-8.x.x.jar`（官网 Connector/J，或 Maven `com.mysql:mysql-connector-j`）。

### 2. 改配置

打开 `db-opengauss.properties`（或 `db-mysql.properties`），把 `password=` 补上。
openGauss 的地址按你实验里的来：`192.168.98.129:7654`，用户 `test_user`。

### 3. 运行

**IDEA 里**：把 jar 加到 `File → Project Structure → Libraries`，右键 `DbCheck` → Run，
`Program arguments` 填 `db-opengauss.properties`。

**命令行**（Windows 的 `-cp` 用分号，不是冒号）：

```bat
javac -encoding UTF-8 -cp ".;..\..\lib\opengauss-jdbc-5.1.0-og.jar" DbCheck.java
java -cp ".;.;..\..\lib\opengauss-jdbc-5.1.0-og.jar" DbCheck db-opengauss.properties
```

> 命令行里中文可能显示成乱码，那是 Windows 控制台默认 GBK 的显示问题，程序本身没问题；
> 在 IDEA 控制台里看是正常的。想让 cmd 也显示正常，先执行 `chcp 65001`。

## 三、常见报错对照表

| 报错关键字 | 原因 | 处理 |
|---|---|---|
| `No suitable driver found` | 驱动 jar 没在 classpath 里 | 加 jar；确认 URL 前缀与驱动匹配（`jdbc:opengauss:` 配 `org.opengauss.Driver`） |
| `Connection refused` | VM 没开 / 端口没通 / 只监听 127.0.0.1 | 先 `ping 192.168.98.129`；确认 VMware 网络模式（NAT 要端口转发，桥接要同网段）；openGauss 改 `listen_addresses` 与 `pg_hba.conf` |
| `Invalid or unsupported by client SCRAM mechanisms` | openGauss 的 sha256 认证与普通 PG 驱动不匹配 | 换官方 `opengauss-jdbc`；或服务端 `pg_hba.conf` 改认证方式后 reload |
| `Public Key Retrieval is not allowed` | MySQL 8 默认认证插件 | URL 加 `allowPublicKeyRetrieval=true` |
| `The server time zone value ... is unrecognized` | 没设时区 | URL 加 `serverTimezone=Asia/Shanghai` |
| `Unknown database 'xxx'` | 库名不存在 | 先建库；openGauss 不要连不存在的库 |
| `permission denied for schema public` | 该用户没有建表权限 | 用有权限的用户，或先 `GRANT` 到自己建的 schema |
| 中文变 `???` | 客户端/连接字符集不是 UTF-8 | URL 加 `characterEncoding=utf8`；openGauss 建库时指定 `ENCODING 'UTF8'` |

## 四、两个只会在课设里咬你的坑

1. **openGauss 不支持跨库查询**。你在实验三必须开两个窗口查 `school2` 和 `supplydb`，就是这个原因。
   课设务必**一个库装全部表**（例如建 `hospital` 库），不要像实验那样拆成两个库——跨库 JOIN 写不出来。
2. **openGauss 会把未加引号的标识符折成小写**（PostgreSQL 规则），MySQL 在 Windows 上大小写不敏感。
   统一用「小写下划线」命名（`reg_order`、`schedule_slot`），别用驼峰，也别加双引号。

## 五、答辩前的保险

你的库全在 VMware 里。答辩当天 VM 起不来、快照坏了、主机休眠过——都是真实发生过的事。建议：

1. 答辩前一天给 VM 打快照，并导出一次 `pg_dump`（或 `mysqldump`）到 Windows 本地。
2. 如果 DDL 只用兼容子集（`INT` / `VARCHAR` / `DATE` / `DECIMAL` / 主外键 / `CHECK` / `UNIQUE` / `INDEX` / `VIEW`），
   在 Windows 本机装一个免安装版 MySQL 8 就能跑同一套库，等于白送一份兜底。
3. 演示前把所有脚本固化成 `01_schema.sql` / `02_data.sql`，**一条命令能重建整个库**。
