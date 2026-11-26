# 医院挂号系统

软件系统综合设计课程设计 · 4 人小组 · Java Swing + openGauss

面向医院门诊的**挂号—收费—就诊**闭环管理系统：管理员维护科室、医生与排班号源，挂号员建档挂号收费，医生查看当日患者并确认就诊，管理员查看四类统计报表。

---

## 一、功能概览

22 条功能，按角色与模块划分（P0 必须 / P1 应该 / P2 可选）。

| 模块 | 功能 |
|---|---|
| 基础资料 | F01 患者建档 · F02 患者查询与修改 · F03 科室维护 · F04 医生维护 · F21 登录与权限 · F22 用户管理 |
| 排班号源 | F05 排班生成 · F06 号源生成与停诊 · F07 排班查询 |
| 挂号收费 | F08 挂号 · F09 挂号费计算 · F10 挂号单查询 · F11 退号 · F12 号源回滚 · F13 收费登记 · F14 日结汇总 |
| 医生工作 | F15 医生工作台 · F16 就诊确认 |
| 统计报表 | F17 挂号量统计 · F18 号源使用率 · F19 退号率 · F20 收入统计 |

### 角色权限

| 角色 | 可见菜单 |
|---|---|
| 管理员 | 全部 16 项 |
| 挂号员 | 患者管理、挂号、退号、收费、日结汇总 |
| 医生 | 今日挂号、就诊确认 |

### 默认账号

| 用户名 | 密码 | 角色 |
|---|---|---|
| `admin` | `123456` | 管理员 |
| `guahao` / `guahao2` | `123456` | 挂号员 |
| `doctor1` | `123456` | 医生（关联张明远） |

密码以 SHA-256 摘要存储，不存明文。

---

## 二、技术栈

| 项 | 版本 / 说明 |
|---|---|
| JDK | 26（不用 preview 特性） |
| GUI | Swing + **FlatLaf 3.8**（浅色主题，视觉参数集中在 `UiTheme`） |
| 数据访问 | 原生 JDBC + `PreparedStatement`，无 ORM |
| 数据库 | openGauss（GaussDB Kernel V500R002C00，`server_version` 报 9.2.4） |
| 驱动 | `lib/opengauss-jdbc-5.1.0-og.jar`，驱动类 `org.opengauss.Driver` |
| 构建 | 纯 IDEA 工程，无 Maven / Gradle，依赖放 `lib/` |

---

## 三、目录结构

```
Java_Database_Design/
├─ lib/                         依赖 jar（随仓库分发）
│  ├─ opengauss-jdbc-5.1.0-og.jar
│  └─ flatlaf-3.8.jar
├─ sql/
│  ├─ 01_schema.sql             建表 + 约束 + 索引（含 DROP，可重复执行）
│  └─ 02_data.sql               造数：科室10/医生40/患者300/排班60/号源1800/挂号1000/收费901/用户4
├─ docs/                        需求、表清单、分工、协作说明
├─ tools/环境验证/              DbCheck.java：JDBC 七项自检与报错对照表
└─ src/
   ├─ db.properties.example     连接配置模板（复制为 db.properties 后填密码）
   └─ hospital/
      ├─ App.java               程序入口（先装 FlatLaf，再应用 UiTheme）
      ├─ entity/                8 张表对应实体 + stat/ 下 3 个统计 DTO
      ├─ util/DBUtil.java       连接与资源释放
      ├─ dao/                   9 个 DAO（8 张表 + StatsDao 聚合查询）
      ├─ service/               9 个 Service + BusinessException（事务与业务校验）
      └─ ui/
         ├─ LoginFrame / MainFrame / Session
         ├─ common/             UiUtil、TableUtil、DateSpinner、UiTheme
         └─ module/             13 个业务面板 + dialog/ 3 个对话框
```

---

## 四、快速开始

**1. 准备 JDK 与依赖**

- IDEA 中 `Project Structure → SDK` 选 JDK 26
- `Project Structure → Libraries` 加入 `lib/` 下两个 jar（缺 `flatlaf` 会报找不到 `FlatLightLaf`）
- 把 `src` 标记为 Sources Root

**2. 配置数据库连接**

复制 `src/db.properties.example` 为 `src/db.properties`，填入密码：

```properties
driver=org.opengauss.Driver
url=jdbc:opengauss://192.168.98.129:7654/hospital
user=test_user
password=你的密码
```

`src/db.properties` 已被 `.gitignore` 排除，不会提交。

**3. 初始化数据库**

在 openGauss 中确认存在 `hospital` 库（编码 UTF8），然后**按顺序**执行：

```
sql/01_schema.sql    ← 会先 DROP 再建表，已有数据会被清空
sql/02_data.sql      ← 只能在 01 之后执行一次，重复执行会撞 dept_name 唯一约束
```

**4. 运行**

运行 `hospital.App` 的 `main`，用上表账号登录。

**5. 自检（可选）**

`tools/环境验证/DbCheck.java` 做 7 项检查（驱动、连接、查询、建表、事务回滚、提交与中文、主键约束），全 `[OK]` 说明环境正常。

---

## 五、分层架构

```
ui/       界面层   显示数据、收集输入、弹提示
  ↓
service/  业务层   校验规则 + 事务边界
  ↓
dao/      数据层   只写 SQL，一个方法一条查询
  ↓
entity/   实体层   一个对象 = 表里一行
  ↓
        openGauss
```

`util/DBUtil` 只负责给 Service 提供连接。

**关键约定**：DAO 的所有方法第一个参数都是 `Connection`，DAO 内部不取连接、不提交、不回滚。连接与事务全部由 Service 管理——因为一次挂号要动 3 张表，必须共用一个事务。

### 一条完整调用链（挂号）

```
RegisterPanel 收集 patientId、slotId、operatorId
  ↓
RegistrationService.register(...)
  ├─ conn.setAutoCommit(false)
  ├─ 校验患者 / 号源 / 排班 / 停诊 / 是否过期
  ├─ SlotDao.markUsed(conn, slotId)   ← 返回 false 立即抛"该号已被挂走"
  ├─ RegistrationDao.insert(...)      ← RETURNING 取回主键
  └─ conn.commit()
```

---

## 六、数据库约定（踩过的坑）

### 1. `VARCHAR(n)` 的 n 是**字节**，不是字符

库为 `DBCOMPATIBILITY=A`（Oracle 兼容模式），实测 `VARCHAR(20)` 存 7 个汉字报 `22001`。

| 用途 | 类型 |
|---|---|
| 编号、状态码 | `VARCHAR(20)` |
| 姓名、科室名、职称 | `VARCHAR(60)` |
| 地址、备注 | `VARCHAR(200)` |
| 金额 | `NUMERIC(10,2)` |
| 日期 / 时间 | `DATE` / `TIMESTAMP` |

> 1 个汉字占 3 字节，中文列按 3 倍预留。

### 2. 日期比较不能用 CAST

本库 `CAST(timestamp AS date)` **不会截断时间**（Oracle 兼容语义），`WHERE CAST(reg_time AS date) = CURRENT_DATE` 永远返回 0 行。

```sql
-- 正确
WHERE reg_time >= ? AND reg_time < ?        -- 传当天与次日
WHERE TO_CHAR(reg_time,'YYYY-MM-DD') = ?
```

> 注意：SQL 层的 `CAST` 有问题，但 JDBC 层 `rs.getDate()` 对 DATE 列是正常的，`DBUtil.toLocalDate` 可放心用。

### 3. 内核是 PG 9.2，这些写法不可用

| 禁止 | 替代 |
|---|---|
| `GENERATED ALWAYS AS IDENTITY` | `SERIAL` |
| `ON CONFLICT (...)` | `ON DUPLICATE KEY UPDATE` |
| `COUNT(*) FILTER (WHERE ...)` | `SUM(CASE WHEN ... THEN 1 ELSE 0 END)` |
| `LATERAL` | 子查询 / 相关子查询 |
| 双引号标识符、驼峰表名 | 全小写下划线（未加引号会被折成小写） |
| 跨库查询 `库名.表名` | 单库 `hospital`，表都在 `public` |

可用：窗口函数、`WITH ... INSERT`、`string_agg`、`LIMIT/OFFSET`、`DATE - DATE`（得整数天）。

---

## 七、关键业务口径

| 规则 | 说明 |
|---|---|
| **重复挂号防重** | 靠 `registration.slot_id` 的 UNIQUE 约束 + `SlotDao.markUsed` 的条件更新（`WHERE status='可挂'`），返回 0 行即表示被抢。**不允许先查后插** |
| **退号条件** | 仅「已挂号且未就诊」，且**就诊日期不早于今天**（今天与未来号可退，过期号不可退） |
| **退号事务** | 挂号单置已退号 + 号源释放 + 收费单退费，三张表同一事务，任一失败整体回滚 |
| **挂号费快照** | 挂号时把医生当前挂号费复制进 `registration.reg_fee`，医生调价不影响历史单据（有意冗余，不违反 3NF） |
| **停诊** | 排班置停诊后，未挂号的号源批量置「停用」；已挂号的不自动退，需人工办理 |
| **收费限制** | 已退号的单不能收费；停诊排班的单不能收费；一张挂号单只能收费一次 |
| **剩余号数** | 不存字段，用 `COUNT(*) WHERE status='可挂'` 实时计算 |
| **号源使用率** | = 已挂数 ÷ 号源总数（**停诊号源计入分母**） |
| **查询上限** | 患者查询最多返回 200 条，挂号单查询最多 500 条，界面不提示 |

---

## 八、开发与协作

- 分支：`main` 只放能跑的版本；个人开 `feat/<模块>`，每天合并一次
- 提交信息带前缀：`[A] 完成排班生成 F05`，便于导出分工证据
- 表结构只由 A 修改，改完必须同步 `sql/01_schema.sql` 并通知全组
- 不要在 DBX / Navicat 里手改表结构，也不要用 `DROP`
- 数据库重置：`01 → 02`，顺序不能反

---

## 九、文档索引（`docs/`）

| 文件 | 内容 |
|---|---|
| `01_需求一页纸.md` | 角色、22 条功能、验收场景、范围与排除项 |
| `02_表清单.md` | 8 张表字段与约束、关系清单、三个设计要点 |
| `03_分工表.md` | 四人分工、协作约定、进度咬合点、答辩预演问题 |
| `05_开发提示词.md` | 分批生成代码用的提示词（含环境与方言约束） |
| `06_协作接入说明.md` | 组员克隆、配置、环境验证的步骤 |

`tools/环境验证/README.md` 另附常见报错对照表（驱动未加、连接拒绝、认证失败、时区、中文乱码等）。

---

## 十、已知限制

- 单机桌面程序，无服务端；数据库由一台机器承载，其他人通过局域网连接
- 不做并发压测，防重依赖数据库约束而非锁机制
- 无患者自助端、无在线支付、无电子病历、无药品库存（刻意排除，见需求一页纸）
- 报表只到「科室」粒度，不做医生个人绩效
- 查询结果按上限截断，没有分页
- 复杂查询清单尚未单独整理到 `sql/99_query.sql`（当前复杂查询分散在各 DAO 与 `StatsDao` 中，报告需用时再汇总）

---

## 十一、环境备注

- 数据库：`192.168.98.129:7654`（VMware 中的 openEuler + openGauss），库 `hospital`
- 库属性：`ENCODING=UTF8`、`LC_COLLATE=C`、`DBCOMPATIBILITY=A`
- 连接用户：`test_user`（非超管，有 CREATEDB 权限，可建库建表）
- 打包：IDEA `Artifacts` 导出可运行 jar 时，**必须把 `lib/` 下两个 jar 一起打进去**，否则运行报 `NoClassDefFoundError`
