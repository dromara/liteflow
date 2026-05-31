# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在本仓库中工作时提供指引。

## 概述

LiteFlow（v2.16.0）是一个轻量级规则引擎框架，用于复杂的组件化业务编排。它通过 DSL 驱动工作流，支持热重载和 11 种脚本语言。项目面向 Java 8+（最高支持到 JDK 25），拥有 2000+ 测试用例。

**官方文档**：https://liteflow.cc/pages/5816c5/

## 命令

### 构建
```bash
# 构建整个项目（模块集由 JDK 版本自动选择 —— 见下方说明）
mvn clean package -DskipTests

# 带测试构建
mvn clean package

# 构建指定模块
mvn clean package -DskipTests -pl liteflow-core

# 发布生产模块（手动选择 profile，不会自动激活）
mvn clean package -DskipTests -P release-on-8     # JDK 8 发布产物
mvn clean package -DskipTests -P release-on-17     # react-agent + spring-boot4-starter
```

**JDK 驱动的模块选择（重要）：** 根 `pom.xml` 中没有顶层的 `<modules>` 块 —— 构建的模块列表完全由按 JDK 激活的 profile 提供：
- `compile-8-to-16`（在 JDK `[1.8,17)` 上激活）：core、scripts、rules、spring-boot-starter、spring、solon、testcase-el、el-builder、benchmark。
- `compile-17+`（在 JDK `[17,)` 上激活）：以上全部，**外加** `liteflow-react-agent` 和 `liteflow-spring-boot4-starter`。

因此 `liteflow-react-agent` 和 `liteflow-spring-boot4-starter` 只有在 JDK 17+ 上构建时才会被纳入。这两个模块以 `maven.compiler.target=17` 编译，但根 pom 注释指出 agentscope-java（react-agent 背后的引擎）**运行时需要 Java 21+** —— 本仓库当前的开发 JDK 为 21。`release-on-8` / `release-on-17` 这两个 profile 没有 `<activation>`，必须用 `-P` 手动选择。

### 运行测试
```bash
# 运行所有测试
mvn test

# 运行指定模块的测试（liteflow-testcase-el/ 下有 30+ 个测试模块）
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-springboot

# 运行单个测试类
mvn test -pl liteflow-core -Dtest=FlowExecutorTest

# 运行指定的测试方法
mvn test -pl liteflow-core -Dtest=FlowExecutorTest#testExecute
```

### 其他命令
```bash
# 依赖树
mvn dependency:tree

# 查看模块结构
ls liteflow-*/pom.xml
```

## 高层架构

### 核心执行模型

**FlowExecutor** → **Chain** → **Condition Tree（条件树）** → **Node Components（节点组件）**

1. **FlowExecutor**：执行工作流的入口（`execute2Resp(chainId, param)`）
2. **FlowBus**：所有 chain 和 node 的中央元数据注册中心（线程安全，支持热重载）
3. **Chain**：由 EL 表达式组成的命名工作流，编译为一棵 Condition 树
4. **Slot/DataBus**：基于 slot 池的线程安全上下文管理（`slotSize` 可配置）
5. **NodeComponent**：所有业务逻辑组件的基类

### 关键架构模式

#### 1. 两阶段解析
为处理循环依赖，chain 的构建分两个阶段进行：
- **阶段 1**：注册 chain ID（创建占位 chain）
- **阶段 2**：通过 EL 解析构建完整的条件树

#### 2. EL 表达式语言
使用 QLExpress 解析声明式工作流。示例算子：
- **THEN(a, b, c)**：顺序执行
- **WHEN(a, b, c)**：并行执行（异步）
- **IF(condition, trueNode, falseNode)**：条件分支
- **SWITCH(selector).to(a, b, c)**：多路分支
- **FOR(count).DO(loop)**：固定次数循环
- **WHILE(condition).DO(loop)**：条件循环
- **ITERATOR(iterator).DO(loop)**：迭代器循环
- **RETRY(node).times(3).forException(Ex.class)**：重试机制
- **CATCH(node).DO(handler)**：异常处理
- **TIMEOUT(node).time(1000)**：执行超时（毫秒）
- **PRE(a, b)**：前置条件（始终在 chain 之前执行，出错也会执行）
- **FINALLY(a, b)**：finally 条件（始终在 chain 之后执行）
- **AND(a, b)**、**OR(a, b)**、**NOT(a)**：用于 IF 条件的布尔逻辑
- **node.tag("t")**、**.data("k","v")**、**.id("id")**：节点级修饰符

规则在 XML/JSON/YML 中定义：
```xml
<chain name="myChain">
    THEN(a, WHEN(b, c).maxWaitSeconds(5), IF(e, f, g));
</chain>
```

#### 3. 组件类型
均继承 `NodeComponent`，但具有专门的行为：
- **NodeComponent**：标准同步组件（`process()` 方法）
- **NodeBooleanComponent**：为 IF 条件返回 boolean（`processBoolean()`）
- **NodeSwitchComponent**：为 SWITCH 路由返回 string（`processSwitch()`）
- **NodeIteratorComponent**：为 ITERATOR 结构提供迭代逻辑
- **NodeForComponent**：控制 FOR 循环行为
- **ScriptComponent**：基于脚本的组件（Groovy、JS、Python 等）

**声明式组件模式**：任何 Spring bean 都可以在不继承基类的情况下成为组件，方式是使用 `@LiteflowCmpDefine`（类级别，指定 `NodeTypeEnum`）和 `@LiteflowMethod`（方法级别，映射到 `LiteFlowMethodEnum`）。这样可以摆脱类继承层级的约束。

**组件生命周期钩子**（在 NodeComponent 子类中覆写，或通过 `@LiteflowMethod` 实现）：
- `isAccess()` – 执行前的准入检查；返回 `false` 则跳过
- `beforeProcess()` / `afterProcess()` – 每个组件的前/后置钩子
- `onSuccess()` / `onError()` – 结果回调
- `isContinueOnError()` – 当该节点失败时 WHEN 是否继续
- `isEnd()` – 标记该节点之后应停止 chain
- `rollback()` – 失败时按逆序调用

**`@FallbackCmp`**：标注一个兜底组件，当主组件不存在或抛异常时启用。

#### 4. 基于 Slot 的上下文
线程安全的执行上下文：
- `DataBus.offerSlot(chainId)` 从池中获取一个 slot
- Slot 包含执行元数据、上下文 bean 和步骤跟踪
- `DataBus.releaseSlot(slotIndex)` 将 slot 归还到池
- 池大小通过 `slotSize` 属性配置

#### 5. 解析模式策略
三种模式（`ParseModeEnum`）：
- **PARSE_ALL_ON_START**：启动时解析所有 chain（默认）
- **PARSE_ONE_ON_FIRST_EXEC**：每个 chain 在首次使用时惰性解析（启动更快）
- **PARSE_ALL_ON_FIRST_EXEC**：在任意 chain 首次执行时解析全部 chain

### 模块结构

#### 核心模块
- **liteflow-core**：核心引擎（FlowExecutor、FlowBus、DataBus、Slot、Condition 系统、组件模型）
- **liteflow-el-builder**：基于 QLExpress 的编程式 chain 构建器 API

#### 规则源插件（`liteflow-rule-plugin/` 下 6 种实现）
- **liteflow-rule-zk**：ZooKeeper 配置源
- **liteflow-rule-sql**：SQL 数据库配置源
- **liteflow-rule-nacos**：Nacos 配置中心
- **liteflow-rule-etcd**：etcd 配置源
- **liteflow-rule-apollo**：Apollo 配置中心
- **liteflow-rule-redis**：Redis 配置源

#### 脚本插件（`liteflow-script-plugin/` 下 11 种语言）
- **liteflow-script-groovy**：Groovy 脚本
- **liteflow-script-javascript**：Rhino JavaScript（JSR223）
- **liteflow-script-graaljs**：GraalVM JavaScript
- **liteflow-script-qlexpress**：阿里 QLExpress
- **liteflow-script-python**：Jython（JVM 上的 Python）
- **liteflow-script-lua**：LuaJ
- **liteflow-script-aviator**：Aviator 表达式语言
- **liteflow-script-java**：Janino（Java 编译器）
- **liteflow-script-javax**：JSR223 标准 Java 编译器
- **liteflow-script-javax-pro**：基于 Liquor 的 Java 编译器（增强版）
- **liteflow-script-kotlin**：Kotlin 脚本

#### 框架集成
- **liteflow-spring**：Spring 框架集成（组件扫描、bean 生命周期、AOP）
- **liteflow-spring-boot-starter**：Spring Boot 2/3 自动配置，使用 `@ConfigurationProperties`
- **liteflow-spring-boot4-starter**：Spring Boot 4 starter（仅 JDK 17+ —— 在 `compile-17+` profile 下构建）
- **liteflow-solon-plugin**：Solon 框架集成（Spring 的轻量替代方案）

#### ReAct Agent（`liteflow-react-agent/`，仅 JDK 17+）
让一个 LLM ReAct agent（由 agentscope-java 驱动）作为普通 LiteFlow 节点编排进 EL 链路。这是一个聚合模块，包含一个 core 模块 + 每个模型供应商各一个模块：
- **liteflow-react-agent-core**：`ReActAgentComponent`（一个 `process()` 为 `final` 的 `NodeComponent`，通过 `model()`、`systemPrompt()`、`userPrompt()`、`tools()`、`handleReply()` 等受保护钩子定制行为）、`ModelSpec` 凭据/模型抽象、conversation/agentKey **会话模型**（`AgentSessionManager`）、可插拔的 memory 持久化（通过 `AgentSessionFactory` SPI 支持 JVM/NONE/LOCAL_FILE/REDIS/MYSQL）、桥接为 LiteFlow `FlowEvent` 的流式事件、workspace 文件工具，以及受管 shell 工具。
- **liteflow-react-agent-openai / -anthropic / -gemini / -dashscope**：各供应商的入口类（如 `OpenAI`、`DeepSeek`、`Kimi`、`Anthropic`、`Gemini`、`DashScope`），返回对应供应商的 `ModelSpec` 子类型。业务应用通常只依赖其中一个供应商模块（每个都会传递依赖 `-core`）。

两层标识：`conversationId`（业务/对话维度，决定 workspace 子目录，整条 chain 内一致）和 `agentKey`（组件维度，默认取 `nodeId`，隔离各 agent 的 memory）。**完整使用指南：`docs/liteflow-react-agent-guide.md`** —— 修改 agent 行为前请查阅它，不要在此处复制其配置表格。

#### 测试基础设施
`liteflow-testcase-el/` 下有 30+ 个测试模块，按以下维度组织：
1. **框架**：springboot、springnative、solon、nospring
2. **配置源**：zk、nacos、etcd、apollo、redis、sql
3. **脚本**：每种语言一个模块 + 多语言混合场景
4. **特性**：builder、declare、routechain、monitoring、timeout 等

测试范式：
```java
@SpringBootTest
@TestPropertySource(value = "classpath:/application.properties")
public class MyTest {
    @Resource private FlowExecutor flowExecutor;

    @Test
    public void test() {
        LiteflowResponse response = flowExecutor.execute2Resp("chainId", "arg");
        Assertions.assertTrue(response.isSuccess());
    }
}
```

### 重要设计模式

#### 用于扩展的 SPI 模式
广泛用于：
- `ContextAware`：框架抽象（Spring vs 非 Spring）
- `PathContentParser`：自定义文件路径解析
- `CmpAroundAspect`：全局组件生命周期钩子
- `DeclComponentParser`：声明式组件解析
- 各语言的脚本执行器

#### 生命周期钩子
多个扩展点：
- `PostProcessChainBuildLifeCycle`：chain 构建前/后
- `PostProcessNodeBuildLifeCycle`：node 构建前/后
- `PostProcessChainExecuteLifeCycle`：chain 执行前/后
- `PostProcessFlowExecuteLifeCycle`：flow 执行前/后

#### 回滚机制
组件实现 `rollback()` 以在失败时自动回滚（通过 `executeSteps` deque 按逆序执行）。

#### 热重载支持
- `MonitorFile` 监听规则文件变化
- `reloadRule()` 无需重启即可刷新
- Copy-on-write 集合（除非 `fastLoad=true`）防止并发修改

#### 元数据缓存
- `FlowBus` 使用 CopyOnWriteHashMap（或在 `fastLoad` 下使用普通 HashMap）
- EL 的 MD5 缓存以复用表达式
- chain 缓存可配置（`chainCacheEnabled`、`chainCacheCapacity`）

### 关键配置（`LiteflowConfig`）
- **ruleSource**：规则文件位置（支持 XML、JSON、YML）
- **parseMode**：解析策略（影响启动性能）
- **slotSize**：上下文 slot 池大小
- **enableMonitorFile**：规则文件热重载
- **supportMultipleType**：混用 XML/JSON/YML 规则
- **whenMaxWaitTime**：WHEN 并行执行的超时时间
- **fastLoad**：关闭 CopyOnWrite 以加快启动
- **enableVirtualThread**：使用虚拟线程（JDK 21+）

### 关键文件位置
- 核心引擎：`liteflow-core/src/main/java/com/yomahub/liteflow/flow/`
- FlowExecutor：`liteflow-core/src/main/java/com/yomahub/liteflow/core/FlowExecutor.java`
- FlowBus：`liteflow-core/src/main/java/com/yomahub/liteflow/flow/FlowBus.java`
- 组件基类：`liteflow-core/src/main/java/com/yomahub/liteflow/core/NodeComponent.java`
- Condition 系统：`liteflow-core/src/main/java/com/yomahub/liteflow/flow/element/condition/`
- EL 解析器：`liteflow-core/src/main/java/com/yomahub/liteflow/parser/el/`

### 重要约定
- **命名**：组件以 `nodeId` 标识，chain 以 `chainId` 标识
- **线程安全**：大量使用 ThreadLocal 和并发集合
- **快速失败**：解析期校验，并给出详细错误信息
- **流式 API**：chain 构建采用 builder 模式（EL builder）
- **命名空间**：chain 可组织进命名空间
- **版本管理**：通过 flatten-maven-plugin 使用 `${revision}` 占位符（当前 2.16.0）
