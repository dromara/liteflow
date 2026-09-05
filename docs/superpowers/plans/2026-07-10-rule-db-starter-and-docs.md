# Rule-DB 模式（Starter 绑定 + Metadata + 文档）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `liteflow.rule-db.*` 配置在 Spring Boot 2/3、Spring Boot 4、Solon 三处 starter 里绑定到 core 的 `RuleDbConfig`，生成 IDE 可发现的 configuration metadata，并产出完整使用指南。让"引入依赖 + 最少配置"即可开启 Rule-DB 模式。

**Architecture:** 三处 starter 的 `LiteflowProperty` 各加一个 `@NestedConfigurationProperty private RuleDbConfig ruleDb;`（core 属性类，直接复用），各自的属性装配类加一行 `liteflowConfig.setRuleDb(property.getRuleDb());`——与既有 `agent`(AgentConfig) 的接法完全一致。metadata 补 `liteflow.rule-db.*` 条目。文档新增 `docs/liteflow-rule-db-guide.md`，CLAUDE.md 补模块说明。

**Tech Stack:** Spring Boot `@ConfigurationProperties`、`@NestedConfigurationProperty`、Solon `@Inject("${liteflow}")`、Markdown。

**前置：计划 1 已完成**（`RuleDbConfig` 已在 `liteflow-core`）。本计划与计划 2 相互独立，可并行；建议先于计划 1 的 Task 8 与计划 2 的 Task 4 完成，好让那两处集成测试走 properties 绑定而非编程式兜底。

## Global Constraints

- 三处 starter 的接法必须与既有 `agent` 字段**逐行对齐**（同样 `@NestedConfigurationProperty`、同样在装配类 set）。
- 不改 core（`RuleDbConfig` 已就位）。
- 提交信息中文、conventional-commits，结尾 `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`。
- 文档用简体中文；不复制配置表进 CLAUDE.md，只在 CLAUDE.md 放一句指引 + 指向 guide。
- 每个 starter 改动后编译验证：`mvn clean package -DskipTests -pl <该 starter 模块>`。

---

### Task 1: Spring Boot 2/3 starter 绑定 + metadata

**Files:**
- Modify: `liteflow-spring-boot-starter/src/main/java/com/yomahub/liteflow/springboot/LiteflowProperty.java`（加 ruleDb 字段 + getter/setter）
- Modify: `liteflow-spring-boot-starter/src/main/java/com/yomahub/liteflow/springboot/config/LiteflowPropertyAutoConfiguration.java`（setRuleDb）
- Modify: `liteflow-spring-boot-starter/src/main/resources/META-INF/additional-spring-configuration-metadata.json`

**Interfaces:**
- Consumes: `com.yomahub.liteflow.property.RuleDbConfig`（计划 1）。
- Produces: `liteflow.rule-db.*` 在 Boot 2/3 下可绑定。

- [ ] **Step 1: LiteflowProperty 加 ruleDb 字段**

在 `agent` 字段声明之后（约第 112 行 `private ... AgentConfig agent;` 下方）加：

```java
	// Rule-DB模式配置
	@NestedConfigurationProperty
	private com.yomahub.liteflow.property.RuleDbConfig ruleDb;
```

在 `getAgent/setAgent` 附近加 getter/setter：

```java
	public com.yomahub.liteflow.property.RuleDbConfig getRuleDb() {
		return ruleDb;
	}

	public void setRuleDb(com.yomahub.liteflow.property.RuleDbConfig ruleDb) {
		this.ruleDb = ruleDb;
	}
```

- [ ] **Step 2: 装配类 setRuleDb**

`LiteflowPropertyAutoConfiguration.java` 在 `liteflowConfig.setAgent(property.getAgent());`（第 59 行）之后加：

```java
		liteflowConfig.setRuleDb(property.getRuleDb());
```

- [ ] **Step 3: metadata 补条目**

在 `additional-spring-configuration-metadata.json` 的 `properties` 数组内追加（放在 `liteflow.agent.*` 条目群之后，注意与前一条目之间的逗号）：

```json
    {
      "name": "liteflow.rule-db.enabled",
      "type": "java.lang.Boolean",
      "description": "是否启用 Rule-DB 模式（规则/脚本以存储引擎为权威源，JVM 只做有界缓存）。引入 liteflow-rule-db-sql 或 liteflow-rule-db-redis 依赖后默认启用，此项作逃生开关。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.application-name",
      "type": "java.lang.String",
      "description": "多应用共库时的隔离维度。为空时取 spring.application.name。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.cache-capacity",
      "type": "java.lang.Integer",
      "description": "有界缓存容量（按 chain 条数计；脚本随引用计数联动）。默认 500。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.seq-poll-seconds",
      "type": "java.lang.Integer",
      "description": "变更序号轮询间隔（秒）。SQL 建议 3，Redis 有 pub/sub 时仅作兜底建议 30。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.reconcile-seconds",
      "type": "java.lang.Integer",
      "description": "清单对账周期（秒）。覆盖丢通知/变更日志被清理/订阅断线窗口。默认 60。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.preload-chain-ids",
      "type": "java.lang.String",
      "description": "启动预热的 chainId 列表（逗号分隔），抹平关键链路的冷启动延迟尖刺。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.fetch-retry-times",
      "type": "java.lang.Integer",
      "description": "缓存未命中回源拉取的重试次数。默认 3。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.url",
      "type": "java.lang.String",
      "description": "[SQL] 规则库 JDBC URL。为空则复用容器 DataSource。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.username",
      "type": "java.lang.String",
      "description": "[SQL] 规则库用户名（url 直连时）。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.password",
      "type": "java.lang.String",
      "description": "[SQL] 规则库密码（url 直连时）。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.driver-class-name",
      "type": "java.lang.String",
      "description": "[SQL] JDBC 驱动类。为空时从 url 推断。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.datasource-bean-name",
      "type": "java.lang.String",
      "description": "[SQL] 指定复用的 DataSource bean 名（多数据源场景）。为空时按类型自动查找。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.table-prefix",
      "type": "java.lang.String",
      "description": "[SQL] 表名前缀（表名 = 前缀+chain/script/change_log，字段名固定）。默认 lf_。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.auto-init-table",
      "type": "java.lang.Boolean",
      "description": "[SQL] 启动时自动 CREATE TABLE IF NOT EXISTS。默认 false（缺表时报错并给出可复制 DDL）。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.address",
      "type": "java.lang.String",
      "description": "[Redis] Redisson 地址，多地址逗号分隔。单机=1个；哨兵=多个+master-name；集群=多个不配 master-name。为空则复用容器 RedissonClient。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.master-name",
      "type": "java.lang.String",
      "description": "[Redis] 哨兵模式的 master 名（配置即哨兵模式）。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.database",
      "type": "java.lang.Integer",
      "description": "[Redis] 数据库序号。默认 0。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.key-prefix",
      "type": "java.lang.String",
      "description": "[Redis] 键前缀（键 = 前缀:app:xxx）。默认 lf。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    },
    {
      "name": "liteflow.rule-db.redisson-bean-name",
      "type": "java.lang.String",
      "description": "[Redis] 指定复用的 RedissonClient bean 名。为空时按类型自动查找。",
      "sourceType": "com.yomahub.liteflow.property.RuleDbConfig"
    }
```

- [ ] **Step 4: 编译验证 + metadata JSON 合法性**

Run: `mvn clean package -DskipTests -pl liteflow-spring-boot-starter`
Expected: BUILD SUCCESS

再校验 JSON 合法（不依赖 jq 时用 python）：

Run: `python3 -c "import json;json.load(open('liteflow-spring-boot-starter/src/main/resources/META-INF/additional-spring-configuration-metadata.json'))" && echo JSON_OK`
Expected: `JSON_OK`

- [ ] **Step 5: Commit**

```bash
git add liteflow-spring-boot-starter
git commit -m "feat(starter): Boot2/3 绑定 liteflow.rule-db.* + configuration metadata

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: Spring Boot 4 starter 绑定 + metadata

**Files:**
- Modify: `liteflow-spring-boot4-starter/src/main/java/com/yomahub/liteflow/springboot4/LiteflowProperty.java`
- Modify: `liteflow-spring-boot4-starter/src/main/java/com/yomahub/liteflow/springboot4/config/LiteflowPropertyAutoConfiguration.java`
- Modify: `liteflow-spring-boot4-starter/src/main/resources/META-INF/additional-spring-configuration-metadata.json`（若存在；不存在则创建）

**Interfaces:**
- Consumes: `RuleDbConfig`。
- Produces: `liteflow.rule-db.*` 在 Boot 4 下可绑定。

- [ ] **Step 1: LiteflowProperty 加 ruleDb（同 Task 1 Step 1，位置对齐 springboot4 版的 agent 字段第 112 行）**

字段声明：

```java
	// Rule-DB模式配置
	@NestedConfigurationProperty
	private com.yomahub.liteflow.property.RuleDbConfig ruleDb;
```

getter/setter：

```java
	public com.yomahub.liteflow.property.RuleDbConfig getRuleDb() {
		return ruleDb;
	}

	public void setRuleDb(com.yomahub.liteflow.property.RuleDbConfig ruleDb) {
		this.ruleDb = ruleDb;
	}
```

- [ ] **Step 2: 装配类 setRuleDb**

`springboot4/config/LiteflowPropertyAutoConfiguration.java` 在 `liteflowConfig.setAgent(property.getAgent());`（第 59 行）之后加：

```java
		liteflowConfig.setRuleDb(property.getRuleDb());
```

- [ ] **Step 3: metadata**

若 `liteflow-spring-boot4-starter/src/main/resources/META-INF/additional-spring-configuration-metadata.json` 存在，追加与 Task 1 Step 3 完全相同的 18 个条目；若不存在，先运行下面命令确认，再决定新建（内容为 `{"properties":[ ...同上18条... ]}`）：

Run: `ls liteflow-spring-boot4-starter/src/main/resources/META-INF/additional-spring-configuration-metadata.json 2>/dev/null || echo MISSING`

- [ ] **Step 4: 编译验证 + JSON 校验**

Run: `mvn clean package -DskipTests -pl liteflow-spring-boot4-starter`
Expected: BUILD SUCCESS

（该模块仅在 JDK 17+ 的 `compile-17+` profile 下构建；若当前 JDK < 17，此步骤跳过并在提交信息注明"未本地验证，随 CI 17+ 构建"。本仓库开发 JDK 为 21，正常可编译。）

- [ ] **Step 5: Commit**

```bash
git add liteflow-spring-boot4-starter
git commit -m "feat(starter): Boot4 绑定 liteflow.rule-db.* + configuration metadata

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: Solon 插件绑定

**Files:**
- Modify: `liteflow-solon-plugin/src/main/java/com/yomahub/liteflow/solon/config/LiteflowProperty.java`
- Modify: `liteflow-solon-plugin/src/main/java/com/yomahub/liteflow/solon/config/LiteflowAutoConfiguration.java`

**Interfaces:**
- Consumes: `RuleDbConfig`。
- Produces: Solon 下 `liteflow.rule-db.*` 可绑定。

- [ ] **Step 1: LiteflowProperty 加 ruleDb 字段（Solon 无 @NestedConfigurationProperty，直接普通字段）**

在 `agent` 字段（第 105 行 `private ... AgentConfig agent;`）之后加：

```java
	// Rule-DB模式配置
	private com.yomahub.liteflow.property.RuleDbConfig ruleDb;
```

在 `getAgent/setAgent` 附近加 getter/setter：

```java
	public com.yomahub.liteflow.property.RuleDbConfig getRuleDb() {
		return ruleDb;
	}

	public void setRuleDb(com.yomahub.liteflow.property.RuleDbConfig ruleDb) {
		this.ruleDb = ruleDb;
	}
```

- [ ] **Step 2: 装配类 setRuleDb**

`LiteflowAutoConfiguration.java` 在 `liteflowConfig.setAgent(property.getAgent());`（第 56 行）之后加：

```java
		liteflowConfig.setRuleDb(property.getRuleDb());
```

- [ ] **Step 3: 编译验证**

Run: `mvn clean package -DskipTests -pl liteflow-solon-plugin`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add liteflow-solon-plugin
git commit -m "feat(solon): 绑定 liteflow.rule-db.* 到 RuleDbConfig

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: 使用指南文档 + CLAUDE.md 模块说明

**Files:**
- Create: `docs/liteflow-rule-db-guide.md`
- Modify: `CLAUDE.md`（模块结构处补 rule-db-sql / rule-db-redis 一段 + 指向 guide）

**Interfaces:** 无代码接口。

- [ ] **Step 1: 写 docs/liteflow-rule-db-guide.md**

结构与既有 `docs/liteflow-metrics-guide.md`、`docs/liteflow-agent-guide.md` 对齐（上手篇 + 参考篇）。至少覆盖：

1. **它解决什么**：与传统 6 个规则插件的本质区别（存储为权威源 vs 启动拼 XML）；两个收益（多节点最终一致、JVM 内存与规则总量解耦）。
2. **快速上手（SQL）**：引入 `liteflow-rule-db-sql` 依赖 → 三行 url/username/password（或零配置复用容器 DataSource）→ `auto-init-table=true` → 用 `SqlRulePublisher.publishChain(...)` 发布第一条规则 → 执行。
3. **快速上手（Redis）**：引入 `liteflow-rule-db-redis` → 一行 address → `RedisRulePublisher.publishChain(...)` → 执行。
4. **配置参考表**：完整 `liteflow.rule-db.*`（从计划 1 §10 的表誊写，标注 SQL/Redis/通用）。
5. **存储结构参考**：SQL 三张表 DDL（从 `liteflow-rule-db-sql` 的 `sql/ddl-mysql.sql` 引出）、Redis 键结构（含 changelog ZSet）。
6. **发布规范**：发布 API 用法；以及"绕过 API 直接写库/写 redis"必须遵守的规范（version+1、写 change_log/ZADD changelog、PUBLISH/INCR seq），并说明 content_md5 对账双保险。
7. **一致性与收敛**：收敛窗口 = max(通知延迟, seq 轮询周期, 对账周期)；三条腿（推送/轮询/对账）。
8. **内存与性能**：索引常驻 + 有界缓存 + 分级刷新（惰性失效实现注记）；`cache-capacity`/`preload-chain-ids` 调优。
9. **降级语义**：存储不可用时缓存命中仍执行；未命中报 `ChainLoadException`。
10. **限制**：与 `rule-source` 互斥；两个 rule-db 插件不能同时在 classpath；v1 不含 zk/nacos/etcd/apollo 的 rule-db 实现、不含 instanceId 持久化。

文档正文以设计文档 `docs/superpowers/specs/2026-07-10-rule-db-plugin-design.md` 为事实源，但**面向使用者**重写（不是把 spec 搬过来）——spec 讲"为什么这么设计"，guide 讲"怎么用"。

- [ ] **Step 2: CLAUDE.md 补模块说明**

在"规则源插件"小节（`liteflow-rule-plugin/` 下 6 种实现）之后，加一段：

```markdown
#### Rule-DB 模式插件（存储为权威源，根级独立父模块 `liteflow-rule-db/` 下 2 种）
- **liteflow-rule-db-sql** / **liteflow-rule-db-redis**：与上面 6 个"启动拼 XML"式插件不同，Rule-DB 模式让规则/脚本**真正以 SQL/Redis 为权威源**，JVM 只保留「id→版本戳」常驻索引 + 有界缓存（编译产物/EL/脚本源码按需拉取、淘汰退影子）。通过 core 的 `RuleRepository` SPI（`com.yomahub.liteflow.repository`）接入，变更经"pub/sub 推送 + 序号轮询 + 周期对账"三条腿收敛，保证多节点最终一致（秒级窗口），且 JVM 内存占用与规则总量解耦。与 `rule-source` 互斥，两个插件不可同时在 classpath。配置命名空间 `liteflow.rule-db.*`。**完整使用指南：`docs/liteflow-rule-db-guide.md`**。
```

- [ ] **Step 3: Commit**

```bash
git add docs/liteflow-rule-db-guide.md CLAUDE.md
git commit -m "docs(rule-db): 使用指南 + CLAUDE.md 模块说明

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 5: 三处 starter 绑定的回归验证

**Files:** 无新增；验证计划 1/2 的 properties 驱动测试现在能走绑定路径。

- [ ] **Step 1: 若计划 1 的 Task 8 已完成，回归 SQL 集成测试（properties 绑定生效）**

Run: `mvn test -DskipTests=false -pl liteflow-testcase-el/liteflow-testcase-el-rule-db-sql-springboot`
Expected: PASS（此时 `liteflow.rule-db.*` 由 Boot2 starter 绑定，不再需要编程式兜底；若测试里仍有编程式兜底，可删除并复跑确认）

- [ ] **Step 2: 若计划 2 的 Task 4 已完成，回归 Redis 集成测试**

Run: `mvn test -DskipTests=false -pl liteflow-testcase-el/liteflow-testcase-el-rule-db-redis-springboot`
Expected: PASS（或按 redis 门控跳过）

- [ ] **Step 3: 全量构建冒烟**

Run: `mvn clean package -DskipTests -pl liteflow-spring-boot-starter,liteflow-spring-boot4-starter,liteflow-solon-plugin`
Expected: BUILD SUCCESS

- [ ] **Step 4: 无代码改动则无需提交；若删除了编程式兜底，提交**

```bash
git add liteflow-testcase-el
git commit -m "test(rule-db): 集成测试改走 starter 属性绑定，移除编程式兜底

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## 计划 3 完成校验

```bash
mvn clean package -DskipTests -pl liteflow-spring-boot-starter,liteflow-spring-boot4-starter,liteflow-solon-plugin
python3 -c "import json;json.load(open('liteflow-spring-boot-starter/src/main/resources/META-INF/additional-spring-configuration-metadata.json'))" && echo JSON_OK
```

预期：三处 starter 构建成功 + metadata JSON 合法 + `liteflow.rule-db.*` 在 IDE 中可自动补全。

## 三个计划的整体交付

- **计划 1**：core 运行时（SPI/索引/缓存/同步/对账）+ `liteflow-rule-db-sql` + SQL 端到端测试。
- **计划 2**：`liteflow-rule-db-redis`（Redisson + ZSet changelog + Lua 原子发布 + pub/sub）+ embedded-redis 测试。
- **计划 3**：三处 starter 绑定 + metadata + 使用指南 + CLAUDE.md。

推荐执行顺序：计划 1 → 计划 3 → 计划 2（计划 3 先于计划 2 可让 redis 集成测试直接走 properties 绑定）。计划 1 的 Task 8 若在计划 3 之前跑，用其内注明的编程式兜底。
