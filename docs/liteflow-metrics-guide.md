# LiteFlow 指标使用指南

LiteFlow 能把每条 chain、每个 node 的执行情况（跑了多少次、多快、错了多少、当前有多少在并发执行）变成可观测的指标，并对接到 Prometheus / Grafana 这类业界标准的监控系统里看曲线、配告警。

本文分两部分：

- **上手篇**：假设你**没接触过 Prometheus / Grafana / Actuator**，带你从零搭一套、把曲线跑出来。先看这部分。
- **参考篇**：每个配置项、每个指标、每条 PromQL 的精确说明，需要查细节时再来。

读完上手篇你应该能：引入依赖让指标自动产生 → 用一份现成的 `docker-compose` 起 Prometheus + Grafana → 在 Grafana 里看到 QPS / 耗时 / 错误率曲线。

> 当前仓库根版本：`2.16.1`。指标能力由框架无关的 `liteflow-metrics` 模块提供，它已是 `liteflow-spring-boot-starter`（Boot 2/3）与 `liteflow-spring-boot4-starter`（Boot 4）的传递依赖，用这两个 starter 的项目**无需单独引入 `liteflow-metrics`**。

---

# 上手篇

## 0. 先搞懂这套东西

如果你只是想知道「我的 chain 跑了多少次、多快、错了多少」，那你要的就是「可观测性（observability）」。这件事不是一个组件搞定的，而是 **4 个角色串成一条流水线**，各管一段：

```
   [1] 你的应用                 [2] Prometheus              [3] Grafana
  LiteFlow + Micrometer  ──抓──▶  定时来抓 + 存起来   ──查──▶  画成曲线 / 配告警
  把指标从一个 HTTP 口            （时间序列数据库）
  暴露出来：
  /actuator/prometheus

  数据流向：应用产生指标 ──(Prometheus 每隔几秒主动来抓一次)──▶ 存进 Prometheus
            ──(Grafana 向 Prometheus 查询)──▶ 仪表盘上的曲线
```

| 角色 | 一句话 | 谁来提供 |
|---|---|---|
| **Micrometer** | 给指标埋点的统一 API（类比日志界的 SLF4J）。LiteFlow 用它记录每次 chain / node 的执行次数和耗时 | LiteFlow 内置（`liteflow-metrics`），你不用管 |
| **Spring Boot Actuator** | 把这些指标通过一个固定 HTTP 地址 `/actuator/prometheus` 吐出来，供外部抓取 | 你加一个 `spring-boot-starter-actuator` 依赖 |
| **Prometheus** | 一个独立进程，每隔几秒主动来「抓」那个 HTTP 地址，把数据按时间存成「时间序列」，并能用 PromQL 查询 | 你自己部署（本文用 docker 一键起） |
| **Grafana** | 连到 Prometheus，把数据画成曲线 / 仪表盘，还能配告警 | 你自己部署（本文用 docker 一键起） |

**记住一句话划清边界**：LiteFlow 只负责把指标「**产生**」并「**暴露**」出来（上面的 [1]）；真正的「抓取、存储、画图」是 Prometheus / Grafana 这两个跟 LiteFlow 完全解耦的开源组件干的。所以下面你会装两样东西——一是给应用加依赖让它吐指标，二是把 Prometheus + Grafana 跑起来。

> 几个会反复出现的词，先有个印象即可，精确定义在参考篇：
> - **registry（注册表）**：Micrometer 里存放指标的容器。你选哪种监控系统，就引入对应的 registry（本文用 Prometheus 的）。
> - **时间序列（time series）**：「某指标 + 一组标签」随时间变化的一串数据点，比如「mChain 的成功执行次数」就是一条时间序列。
> - **抓取（scrape）**：Prometheus 主动来访问你的 `/actuator/prometheus` 拉数据，是「拉」不是「推」。

## 1. 五分钟跑通：从零到看见曲线

下面六步，跑完你就能在 Grafana 看到 LiteFlow 的实时曲线。（细节/特殊场景都在参考篇，这里只走主干。）

### Step 1：让应用产出指标（加 3 个依赖）

除了 LiteFlow starter，再加 `spring-boot-starter-actuator`（提供 HTTP 出口）和一个具体的 registry（这里用 Prometheus 的）：

```xml
<!-- 1) LiteFlow starter（已含 liteflow-metrics） -->
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-spring-boot-starter</artifactId>
    <version>2.16.1</version>
</dependency>
<!-- 2) Actuator：提供 /actuator/* 端点 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<!-- 3) Prometheus registry：指标按 Prometheus 格式吐出 -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

> Spring Boot 4 项目把第 1 个换成 `liteflow-spring-boot4-starter` 即可；Gradle 写法与非 Spring 项目见参考篇 [§2](#2-引入依赖细节)。

### Step 2：打开端点，确认指标已经在吐

加一行配置，把要用的端点对 HTTP 开放（默认只开放 `health`，其余都不暴露）：

```properties
management.endpoints.web.exposure.include=liteflow,prometheus
```

启动应用，验证出口已通（默认端口 8080，按你的 `server.port` 改）：

```bash
curl http://localhost:8080/actuator/prometheus | grep liteflow_
```

只要看到 `liteflow_...` 开头的行，就说明指标已经在产生并暴露了。**此时还没有任何图**——因为没人来抓、没人来画。这正是下一步要做的。

> 看不到 `liteflow_` 行？大概率是端点没暴露（返回 404）或还没跑过任何 chain。排查见参考篇 [§3](#3-开关与端点暴露)。

### Step 3：一键起 Prometheus + Grafana

本仓库已备好一份开箱即用的配置，在 [`docs/metrics-integration/`](./metrics-integration/) 目录。把它拷到你机器上任意位置，进入该目录执行：

```bash
docker compose up -d
```

这会起两个容器：

- **Prometheus**（<http://localhost:9090>）：已配置好，每 5 秒抓一次你的应用
- **Grafana**（<http://localhost:3000>）：已自动挂好 Prometheus 数据源、并自动导入名为「**LiteFlow 概览**」的仪表盘（已开匿名访问，**免登录**）

### Step 4：确认 Prometheus 抓到了你的应用

抓取目标默认是 `host.docker.internal:8080`（容器内访问「宿主机上的应用」的写法，Mac/Windows/Linux 都已适配）。

- 如果你的应用**不在 8080**，或是 `liteflow-example` 这种 8581 端口，编辑 [`prometheus.yml`](./metrics-integration/prometheus.yml) 里的 `targets` 改成你的 `host:port`，然后 `docker compose restart prometheus`。
- 打开 <http://localhost:9090/targets>，看到 `liteflow-app` 这个 target 状态为 **UP** 就对了。

### Step 5：打开 Grafana 看板

浏览器开 <http://localhost:3000>，左侧 Dashboards 里点开「**LiteFlow 概览**」。这块板自带 6 个面板：Chain QPS、Chain 平均耗时、Chain 错误率、Chain 在途执行数、Node 平均耗时、Slot 占用 vs 容量。顶部还有个 `chain` 下拉，可只看某条链路。

### Step 6：打点流量，看曲线动起来

调用几次你的业务接口（让 chain 真正执行），等几秒钟（抓取间隔 + Grafana 刷新），面板上的曲线就会动起来。到这里整条链路就跑通了：

```
你的接口被调用 → chain 执行、LiteFlow 记录指标 → /actuator/prometheus 暴露
   → Prometheus 抓走 → Grafana 查询 → 你看到的曲线
```

> **想要 P95 / P99 分位线？** Timer 默认只发布 count / sum / max，倒推不出分位。开一行直方图配置即可，见参考篇 [§6](#6-分位--直方图)。
>
> **想自己改面板 / 加面板？** 直接在 Grafana 里编辑，或参照参考篇 [§7](#7-常用-promql-与告警) 的 PromQL 自己拼。仪表盘 JSON 在 [`grafana/dashboards/liteflow-dashboard.json`](./metrics-integration/grafana/dashboards/liteflow-dashboard.json)。

上手篇到此结束。下面参考篇是逐项细节，按需查阅。

---

# 参考篇

## 2. 引入依赖（细节）

指标采集依赖一个 `MeterRegistry`（由具体监控系统提供）。所以除了 LiteFlow starter，还需要 `spring-boot-starter-actuator` 和一个具体的 registry（如 `micrometer-registry-prometheus`）。

> `liteflow-metrics` 把 `micrometer-core`、`spring-boot-actuator*` 都标记为 `optional`，不污染你的依赖树；你引入哪个 registry，LiteFlow 就把指标写到哪个 registry。

### Spring Boot 2 / 3 项目

Maven 见上手篇 Step 1。Gradle：

```groovy
implementation 'com.yomahub:liteflow-spring-boot-starter:2.16.1'
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
```

### Spring Boot 4 项目（JDK 17+）

把 starter 换成 `liteflow-spring-boot4-starter`，其余依赖完全一致：

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-spring-boot4-starter</artifactId>
    <version>2.16.1</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### 非 Spring / Solon 项目

`liteflow-metrics` 本身框架无关，但**钩子不会被自动发现**：在 Spring / Solon 下，`ChainMetricsLifeCycle` / `NodeMetricsLifeCycle` 是作为 `LifeCycle` 类型的 Bean 被框架扫描后注册进 `LifeCycleHolder` 的；非 Spring 环境没有这个扫描过程，也没有 SPI 自动加载，**只 `new` 出实例不会产生任何指标**。你需要显式引入 `liteflow-metrics`，然后手动完成两步注册：

```java
MeterRegistry registry = ...;   // 你自己的 MeterRegistry（如 PrometheusMeterRegistry）
LiteflowConfig config = ...;    // 你的 LiteflowConfig

// 1. 把两个执行钩子注册进 LiteFlow，否则它们永远不会被回调
LifeCycleHolder.addLifeCycle(new ChainMetricsLifeCycle(registry));
LifeCycleHolder.addLifeCycle(new NodeMetricsLifeCycle(registry));

// 2. 注册表 / slot 池的 Gauge 需要手动 bindTo
new LiteflowMeterBinder(config).bindTo(registry);
```

完成后，chain / node 指标与全局 Gauge 都会写入你的 registry。结构端点 `/actuator/liteflow` 是 Spring Actuator 的能力，在非 Spring 环境不可用，但指标依然照常采集。

---

## 3. 开关与端点暴露

### LiteFlow 自有开关（仅一个）

```properties
# 可选。默认开启（matchIfMissing=true）。
# 仅在需要临时关闭指标采集时显式设为 false。
liteflow.metrics.enabled=true
```

装配守护条件是：类路径存在 `io.micrometer.core.instrument.MeterRegistry` + 容器中存在 `MeterRegistry` Bean + `liteflow.metrics.enabled` 非 false + liteflow 核心处于启用状态（`liteflow.enable` 非 false，即容器中存在 `FlowExecutor`）。四者同时满足时，`ChainMetricsLifeCycle`、`NodeMetricsLifeCycle`、`LiteflowMeterBinder` 才会被装配。换句话说，**没有引入任何 registry 就不会有任何指标行为**；同理，**`liteflow.enable=false` 关闭核心时指标装配会整体退出**，不会再触发 LiteFlow 内部任何初始化，无需再手动联动 `liteflow.metrics.enabled`。

### 暴露 Actuator 端点

端点暴露属于标准的 Spring Boot Actuator 配置，LiteFlow 不另立配置项：

```properties
# 暴露 liteflow 结构端点 + Prometheus 抓取端点 + 通用 metrics 端点
management.endpoints.web.exposure.include=liteflow,prometheus,metrics
# 可选：通常无需添加。Actuator 端点默认即“启用”，仅当你设了
# management.endpoints.enabled-by-default=false 时才需要显式打开。
# management.endpoint.liteflow.enabled=true
```

加上 `exposure.include` 后，`/actuator/liteflow`、`/actuator/prometheus`、`/actuator/metrics` 即可访问。

#### 为什么必须配 `exposure.include`，不配会怎样

Actuator 把端点分成两个**相互独立**的维度：

- **enabled（启用）**：端点是否存在、bean 是否装配。除 `shutdown` 外**默认都启用**。
- **exposed（暴露）**：是否对 HTTP 开放。出于安全考虑，web 端**默认只暴露 `health`**，其余端点（含 `liteflow` / `prometheus` / `metrics`）默认不对外。

所以 `exposure.include` 是显式 opt-in。**不配的话**：指标照常采集（采集只由 `liteflow.metrics.enabled` + 是否有 `MeterRegistry` 决定，与暴露无关），但这三个端点 HTTP 访问会返回 **404** —— 数据在内存里有，却没有出口，Prometheus 也抓不到。一句话：**采集 ≠ 暴露**。

> 图省事可用 `management.endpoints.web.exposure.include=*` 暴露全部，但**生产不建议** —— 会顺带把 `env` / `configprops` / `heapdump` 等敏感端点也开出去。按需列出更安全。

#### 端点在哪个端口

端点走哪个端口同样是 Actuator 的标准行为，与 `liteflow.metrics.enabled` 无关（该开关只管采集，不监听任何端口）：

| 情况 | `/actuator/liteflow`、`/actuator/prometheus` 等的访问端口 |
|---|---|
| **默认**（未配 `management.server.port`） | 跟应用主端口 **`server.port`**（默认 **8080**）同一个，前缀 `/actuator/*` |
| 配了 `management.server.port=9090` | Actuator 端点走**独立管理端口 9090**，业务接口仍在 `server.port` |

默认情况下访问地址即：`http://localhost:8080/actuator/liteflow`。

---

## 4. 指标目录

所有指标命名前缀为 `liteflow.`，tag 全部为低基数（`chainId` / `nodeId` / 状态 / 异常类 simpleName），不包含 requestId、完整异常 message 等高基数维度。

> Prometheus 抓取时会做命名转换：`.` → `_`；Timer 的基础单位是秒，会带 `_seconds` 后缀；Counter 会带 `_total` 后缀。详见 [§7](#7-常用-promql-与告警) 的 PromQL。

### 4.1 Chain 级指标（按 `chainId` 聚合）

| 指标 | 类型 | Tags | 含义 |
|---|---|---|---|
| `liteflow.chain.executions` | Timer | `chain`, `scope`(main/sub), `status`(success/failed) | chain 执行次数、总/平均/最大耗时、成功数、失败数 |
| `liteflow.chain.active` | LongTaskTimer | `chain` | 当前在途执行数 + 最长在途耗时（发现卡住/堆积） |
| `liteflow.chain.errors` | Counter | `chain`, `exception`(异常类 simpleName) | 按异常类型分布的失败数 |

说明：
- `status` 由 chain 执行结束时 `slot.getException()` 是否为 `null` 判定。
- `scope` 区分主链与子链：EL 中引用的子链每次执行也会单独计一次，所以某条 chain 的总执行数可能大于业务侧的调用次数。统计"业务调用量"时请筛 `scope=main`。
- `exception` tag 取异常类的 `getClass().getSimpleName()`，基数有界。
- 同线程嵌套子链（before A → before B → after B → after A）通过每线程样本栈正确配对；WHEN 并行子链在各自线程，互不影响。

### 4.2 Node 级指标（按 `nodeId` 聚合）

| 指标 | 类型 | Tags | 含义 |
|---|---|---|---|
| `liteflow.node.executions` | Timer | `node`, `type`(NodeTypeEnum.name()), `status` | 组件执行次数、耗时分布、成功/失败 |
| `liteflow.node.active` | LongTaskTimer | `node` | 在途组件数（定位慢/挂起组件） |
| `liteflow.node.errors` | Counter | `node`, `exception` | 组件按异常类型的失败数 |

说明：
- `type` 取 `NodeComponent.getType().name()`，即 `COMMON` / `BOOLEAN` / `SWITCH` / `FOR` / `ITERATOR` / `SCRIPT` 等 `NodeTypeEnum` 枚举名。
- 耗时与异常直接来自节点执行的 `finally` 块，无需订阅方自行计时。

### 4.3 全局 / 注册表 Gauge（`LiteflowMeterBinder` 一次性绑定）

| 指标 | 来源 | 含义 |
|---|---|---|
| `liteflow.chains.registered` | `FlowBus.getChainMap().size()` | 已注册 chain 总数 |
| `liteflow.nodes.registered` | `FlowBus.getNodeMap().size()` | 已注册 node 总数 |
| `liteflow.slot.size` | `LiteflowConfig.slotSize` | slot 池容量 |
| `liteflow.slot.occupied` | `DataBus.OCCUPY_COUNT` | 当前在用 slot 数（关键饱和度，逼近容量即并发吃紧/泄漏） |

---

## 5. 三个端点分别返回什么

开启后你会用到三个 HTTP 端点，定位完全不同，先用一张表区分，再细看 LiteFlow 自有的 `/actuator/liteflow`：

| 端点 | 谁提供 | 数据形态 | 典型用途 |
|---|---|---|---|
| `/actuator/liteflow` | LiteFlow 自有 | 结构定义 + 指标快照（JSON） | 人看 / 排查链路 |
| `/actuator/prometheus` | Actuator + Prometheus registry | **全量**指标，Prometheus 文本格式 | 给 Prometheus 定时抓取 |
| `/actuator/metrics` | Actuator（Micrometer） | 单指标浏览 / 下钻（JSON） | 人看 / 临时调试 |

关键差异：

- **`/actuator/liteflow`** 读 `FlowBus` 结构，能看到 Micrometer 没有的**定义信息**（EL 原文、组件 class/type/language、节点属于哪些 chain），且**包含从未执行过的 chain/node**。详见下面 [5.1](#51-结构端点-actuatorliteflow)。
- **`/actuator/prometheus`** 返回 `# HELP` / `# TYPE` + 样本行的纯文本，包含 JVM/HTTP 等**所有** Micrometer 指标，其中 LiteFlow 部分的命名见 [§7](#7-常用-promql-与告警)。它是机器读的，QPS / 平均 / P95 由 Prometheus 抓走后用 PromQL 算出。
- **`/actuator/metrics`** 用**点号命名**（非下划线），适合手动调试：

  ```
  GET /actuator/metrics                            → 所有指标名清单（names 数组）
  GET /actuator/metrics/liteflow.chain.executions  → 某指标明细（见下）
  ```

  ```json
  {
    "name": "liteflow.chain.executions",
    "measurements": [
      { "statistic": "COUNT",      "value": 1280 },
      { "statistic": "TOTAL_TIME", "value": 7.2 },
      { "statistic": "MAX",        "value": 0.142 }
    ],
    "availableTags": [
      { "tag": "chain",  "values": ["mChain", "subChain"] },
      { "tag": "status", "values": ["success", "failed"] }
    ]
  }
  ```

  > `TOTAL_TIME` / `MAX` 单位是秒（Timer 基础单位）。可按 tag 下钻：
  > `GET /actuator/metrics/liteflow.chain.executions?tag=chain:mChain&tag=status:failed`。

  注意：`/actuator/prometheus` 与 `/actuator/metrics` 只能查到**执行过、产生了 meter** 的 chain/node；想看从没跑过的，用下面的 `/actuator/liteflow`。

### 5.1 结构端点 `/actuator/liteflow`

只读端点，全部为 `@ReadOperation`，数据由 `LiteflowMetaView` 提供（结构读 `FlowBus`，指标快照读 `MeterRegistry`）。它补齐了 Micrometer 看不到的定义信息（EL 原文、组件 class/type、未执行过的 chain/node 等）。

| 路由 | 返回 |
|---|---|
| `GET /actuator/liteflow` | 概览 |
| `GET /actuator/liteflow/chains` | 全部 chain 列表 |
| `GET /actuator/liteflow/nodes` | 全部 node 列表 |
| `GET /actuator/liteflow/chains/{chainId}` | 单 chain 详情 + 指标快照 |
| `GET /actuator/liteflow/nodes/{nodeId}` | 单 node 详情 + 指标快照 + 包含它的 chain 列表 |

> 指标快照是"尽力而为"：当 `liteflow.metrics.enabled=false` 或容器中没有 `MeterRegistry` 时，端点仍返回结构信息，只是 `metrics` 字段省略。

#### 示例：概览

`GET /actuator/liteflow`

```json
{
  "chainsRegistered": 3,
  "nodesRegistered": 12,
  "slotOccupied": 1,
  "chainIds": ["mChain", "subChain", "ifChain"],
  "nodeIds": ["a", "b", "c", "ifNode"]
}
```

#### 示例：chain 列表项

`GET /actuator/liteflow/chains`

```json
[
  {
    "chainId": "mChain",
    "namespace": "default",
    "el": "THEN(a, b);",
    "elMd5": "f1e2d3c4b5a6..."
  }
]
```

#### 示例：单 chain 详情（含指标快照）

`GET /actuator/liteflow/chains/mChain`

```json
{
  "chainId": "mChain",
  "namespace": "default",
  "el": "THEN(a, b);",
  "elMd5": "f1e2d3c4b5a6...",
  "metrics": {
    "count": 1280,
    "failed": 3,
    "errorRate": 0.00234375,
    "meanMs": 5.6,
    "maxMs": 142.0
  }
}
```

#### 示例：node 列表项

`GET /actuator/liteflow/nodes`

```json
[
  {
    "nodeId": "a",
    "name": "A组件",
    "type": "COMMON",
    "script": false,
    "clazz": "com.example.flow.AComponent",
    "language": null
  }
]
```

#### 示例：单 node 详情（含指标快照与所在 chain）

`GET /actuator/liteflow/nodes/a`

```json
{
  "nodeId": "a",
  "name": "A组件",
  "type": "COMMON",
  "script": false,
  "clazz": "com.example.flow.AComponent",
  "language": null,
  "metrics": {
    "count": 1280,
    "failed": 0,
    "errorRate": 0.0,
    "meanMs": 2.1,
    "maxMs": 38.0
  },
  "inChains": ["mChain", "subChain"]
}
```

指标快照字段含义：`count` = 总执行次数；`failed` = 失败次数；`errorRate` = `failed / count`；`meanMs` = 平均耗时（毫秒）；`maxMs` = 最大耗时（毫秒）。

---

## 6. 分位 / 直方图

分位与直方图属于 Micrometer 标准配置，LiteFlow 不另立配置项。`Timer` 默认只发布 `count` / `sum` / `max`，无法直接倒推 P95 / P99，需要按下面任一方式开启。

> 上手篇仪表盘没有放 P95 面板，正是因为它需要先开下面的直方图。开启后，用 [§7](#7-常用-promql-与告警) 的 `histogram_quantile` PromQL 即可在 Grafana 加一个 P95 面板。

### 客户端分位（进程内计算，默认发布到所有 registry）

适用于不想依赖 Prometheus histogram bucket、希望直接拿到分位值的场景：

```properties
# 为 chain 执行耗时启用客户端 P95 / P99
management.metrics.distribution.percentiles[liteflow.chain.executions]=0.95,0.99
# 为 node 执行耗时启用客户端 P95 / P99
management.metrics.distribution.percentiles[liteflow.node.executions]=0.95,0.99
```

### 直方图（发布 bucket，交 Prometheus 用 histogram_quantile 计算）

推荐用法，聚合更准确，且支持跨实例聚合：

```properties
# 为 chain 执行耗时发布 histogram bucket
management.metrics.distribution.percentiles-histogram[liteflow.chain.executions]=true
management.metrics.distribution.percentiles-histogram[liteflow.node.executions]=true
```

可选：自定义 SLO 边界（仅影响 bucket 切分）：

```properties
management.metrics.distribution.slo[liteflow.chain.executions]=50ms,100ms,500ms
```

### 过滤 / 裁剪指标

如果你想去掉某些指标（例如不想要 `active` 这类 LongTaskTimer），自定义一个 `MeterFilter` Bean 即可，这是 Micrometer 的标准机制：

```java
@Configuration
public class MyMeterFilterConfig {
    @Bean
    public MeterFilter dropActiveMetrics() {
        return MeterFilter.deny(id -> id.getName().endsWith(".active"));
    }
}
```

---

## 7. 常用 PromQL 与告警

> 这一节是「自己拼面板 / 配告警」的素材库。上手篇导入的仪表盘里的面板，用的就是这里的 PromQL。

Prometheus 抓取时，指标名会按 Micrometer 约定转换：

- Timer `liteflow.chain.executions` → `liteflow_chain_executions_seconds_count` / `_sum` / `_max` / `_bucket`
- Counter `liteflow.chain.errors` → `liteflow_chain_errors_total`
- LongTaskTimer `liteflow.chain.active` → `liteflow_chain_active_seconds_active_count`（在途任务数）/ `_duration_sum`（在途总耗时）/ `_max`。注意 LongTaskTimer 的后缀是 `_active_count` / `_duration_sum`，**不是** Timer 的 `_count` / `_sum`
- Gauge `liteflow.slot.occupied` → `liteflow_slot_occupied`（tag 名保持原样：`chain` / `node` / `status` / `exception` / `type`）

下面示例假设 chain 名为 `mChain`、组件名为 `a`。

### QPS（每秒执行次数）

```promql
# chain QPS
rate(liteflow_chain_executions_seconds_count{chain="mChain"}[1m])

# node QPS
rate(liteflow_node_executions_seconds_count{node="a"}[1m])
```

### 平均耗时（秒）

```promql
rate(liteflow_chain_executions_seconds_sum{chain="mChain"}[1m])
  / rate(liteflow_chain_executions_seconds_count{chain="mChain"}[1m])
```

### 错误率

```promql
sum(rate(liteflow_chain_executions_seconds_count{chain="mChain", status="failed"}[5m]))
  /
sum(rate(liteflow_chain_executions_seconds_count{chain="mChain"}[5m]))
```

按异常类型分布的失败速率（`liteflow.chain.errors` Counter）：

```promql
sum by (exception) (rate(liteflow_chain_errors_total{chain="mChain"}[5m]))
```

### P95 耗时（需开启 [§6](#6-分位--直方图) 的直方图）

```promql
histogram_quantile(0.95,
  sum by (le) (rate(liteflow_chain_executions_seconds_bucket{chain="mChain"}[5m])))
```

### slot 池饱和度

```promql
liteflow_slot_occupied / liteflow_slot_size
```

### 在途执行数（LongTaskTimer 的 active 数）

```promql
# 当前活跃任务数（LongTaskTimer 用 _active_count，不是 _count）
liteflow_chain_active_seconds_active_count{chain="mChain"}

# 在途任务累计耗时
liteflow_chain_active_seconds_duration_sum{chain="mChain"}
```

### 常用告警规则示例

```yaml
groups:
  - name: liteflow
    rules:
      # chain 错误率 5 分钟内超过 5%
      - alert: LiteflowChainHighErrorRate
        expr: |
          sum by (chain) (rate(liteflow_chain_executions_seconds_count{status="failed"}[5m]))
            /
          sum by (chain) (rate(liteflow_chain_executions_seconds_count[5m]))
            > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "LiteFlow chain {{ $labels.chain }} 错误率过高"

      # slot 池占用超过 80%
      - alert: LiteflowSlotSaturated
        expr: liteflow_slot_occupied / liteflow_slot_size > 0.8
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "LiteFlow slot 池饱和，可能并发吃紧或存在 slot 泄漏"
```

---

## 8. 职责归属与性能

### 8.1 谁算 QPS / 平均 / 分位

LiteFlow 核心**不做任何统计计算**，只上报原始测量值；聚合与推导全部交给 Micrometer 与监控后端：

| 层 | 职责 | 由谁承担 |
|---|---|---|
| LiteFlow 记录 | 每次执行结束 `timer.record(耗时)`；出错 `counter.increment()` | `ChainMetricsLifeCycle` / `NodeMetricsLifeCycle` 钩子，只记原始值 |
| Micrometer 聚合 | 内存中维护 `count`、`totalTime`(sum)、`max` | Micrometer `Timer`（`LongAdder`） |
| Prometheus / Grafana 推导 | QPS、平均耗时、错误率在**查询时**算出 | 监控后端 |

- **QPS**：不落库，由 `rate(count[1m])` 查询时对只增计数求斜率得到。
- **平均耗时**：不落库，由 `rate(sum[1m]) / rate(count[1m])` 当场相除。
- **P95 / P99**：count + sum 无法倒推，需在进程内开分位或发布 histogram bucket 交 Prometheus `histogram_quantile()`——这是 Micrometer / Prometheus 的标准机制，不是 LiteFlow 的能力。

这与现有的 `MonitorBus`（自存最近 N 条、自算平均、只打日志）有本质区别：本模块**零统计计算、零历史存储**，且与 `MonitorBus` 完全独立、互不影响。

### 8.2 性能影响与基数控制

**单次开销：**
- **未引入 `liteflow-metrics` 时接近零。** chain 钩子调用本就存在于 `Chain.execute()`；node 新钩子在 `NodeComponent.execute()` 的 `finally` 中只多一次"空列表"判断，不分配、不遍历。
- **引入后亚微秒级，通常 < 1%。** Meter 实例在模块内按 tag 组合缓存（`ConcurrentHashMap`），每次执行只做一次字符串拼 key 的缓存查找 + `timer.record(...)`（几次 `LongAdder.add`，量级约几十纳秒）、一个小样本对象、每线程栈的一次入/出栈；出错时再有一次 Counter 自增。相对 `NodeComponent.execute()` 本就存在的 `CmpStep`、`StopWatch`、两次 `LOG.info`，以及用户业务逻辑（常含 DB / IO），新增开销可忽略。

**真正的风险点是时间序列基数：**
- 每个 `(chain,scope,status)` / `(node,type,status)` 组合驻留一个小 Meter（几百字节）并导出一条时间序列。
- 选定的 **chain + node 两级** → 内存 ≈ O(chain 数 + node 数)，几千个量级仅几 MB。
- 已规避高基数陷阱：未采用"node-in-chain 三级"组合；`exception` tag 只取类 simpleName，基数有界；不含 requestId、完整异常 message 等。
- 超高吞吐场景下若需进一步压成本，`active`（LongTaskTimer）是最可做成可选 / 移除的一项。

---

## 附录 A：快速核对清单

- [ ] 已引入 `liteflow-spring-boot-starter`（或 Boot4 版）+ `spring-boot-starter-actuator` + 一个 registry（如 `micrometer-registry-prometheus`）；
- [ ] `management.endpoints.web.exposure.include` 包含 `liteflow,prometheus`；
- [ ] `curl /actuator/prometheus | grep liteflow_` 能看到 `liteflow_` 开头的行；
- [ ] `docs/metrics-integration/` 下 `docker compose up -d` 起好 Prometheus + Grafana；
- [ ] Prometheus `/targets` 里 `liteflow-app` 状态为 UP（不 UP 就改 `prometheus.yml` 的 `targets`）；
- [ ] Grafana「LiteFlow 概览」仪表盘打点流量后曲线有变化；
- [ ] （可选）开启 `management.metrics.distribution.percentiles-histogram[liteflow.chain.executions]=true` 以支持 P95 / P99。

## 附录 B：集成资产清单

上手篇用到的开箱即用配置都在 [`docs/metrics-integration/`](./metrics-integration/)：

| 文件 | 作用 |
|---|---|
| `docker-compose.yml` | 一键起 Prometheus + Grafana 两个容器 |
| `prometheus.yml` | 抓取配置（改 `targets` 指向你的应用） |
| `grafana/provisioning/datasources/datasource.yml` | 启动时自动挂好 Prometheus 数据源 |
| `grafana/provisioning/dashboards/dashboards.yml` | 启动时自动导入仪表盘的 provider 配置 |
| `grafana/dashboards/liteflow-dashboard.json` | 「LiteFlow 概览」仪表盘本体（也可单独导入已有 Grafana） |
| `README.md` | 该目录的独立使用说明 |

> 这套配置为**演示用**（匿名访问、弱口令、5s 抓取间隔），请勿直接用于生产。生产请自行加固认证、调整抓取间隔、并按 [§8.2](#82-性能影响与基数控制) 评估基数。
