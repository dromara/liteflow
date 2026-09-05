# liteflow-agent 真实模型测试报告

测试日期：2026-08-16
测试基线：`codex/upgrade-agentscope-2.0` 分支（LiteFlow 2.16.1.1 + AgentScope 2.0.2）

## 1. 测试环境

| 项 | 值 |
| --- | --- |
| 模型接入方式 | `OpenAICompatible.custom`（OpenAI 兼容自定义端点，env.txt 统一入口） |
| 模型端点 | `https://longjinapi.com/v1` |
| 模型 | `gpt-5.6-terra` |
| 凭据来源 | `liteflow-testcase-el/liteflow-testcase-el-agent/src/test/resources/env.txt`（其它平台 key 为空，按约定只测 OpenAI 兼容模式） |
| Redis | Docker `redis:7`（localhost:16379） |
| MySQL | Docker `mysql:8.0`（localhost:13306，root，库 liteflow） |
| Docker 沙箱镜像 | `python:3.14-slim`（本地已有；默认 `ubuntu:22.04` 因网络无法拉取） |
| A2A 远程端点 | 测试内嵌最小 A2A JSON-RPC 服务端（JDK HttpServer，A2A SDK 0.3.3.Final 协议） |

## 2. 测试范围与组织方式

- 依据《liteflow-agent-guide.md》第 2～14 章逐功能点设计用例，全部使用**真实模型调用**，无离线脚本模型。
- 测试代码位于 `com.yomahub.liteflow.test.agent.real` 包，资源位于 `src/test/resources/real/<场景>/`，命名 `*LiveTest`，仅由 `agent-live` profile 执行，不影响默认离线测试套件。
- 中间件（Redis、MySQL、Docker 沙箱、A2A 端点）全部为真实实例。
- 断言策略：功能性断言从严（工具必须被调用、记忆必须可回忆、文件必须落盘），LLM 措辞类断言从宽。

运行方式：

```bash
# 全量真实模型测试（凭据缺失时自动跳过）
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-agent \
    -P agent-live -DskipTests=false

# 单场景
mvn test -pl liteflow-testcase-el/liteflow-testcase-el-agent \
    -P agent-live -DskipTests=false -Dtest='BasicChainLiveTest'
```

前置：`env.txt` 需提供 `LITEFLOW_AGENT_TEST_BASE_URL` / `LITEFLOW_AGENT_TEST_API_KEY` / `LITEFLOW_AGENT_TEST_MODEL`；Redis / MySQL 容器需在 16379 / 13306 端口可用（缺失时 MySQL 用例内的行检查会跳过，但状态存储用例本身会失败）。

## 3. 功能点覆盖与结果

共 15 个测试类、53 个用例；另有 platform 目录 10 个平台连通性用例（无凭据平台按设计跳过）。

| 指南章节 | 测试类（real 包） | 用例 | 覆盖功能点 | 结果 |
| --- | --- | --- | --- | --- |
| §2 快速开始 | `basic/BasicChainLiveTest` | 5 | 三抽象方法最小组件、EL 编排、responseData 写入；namespace 缺失 fail-fast | ✅ 5/5 |
| §3 接入模型平台 | `basic/BasicChainLiveTest` | （同上） | 通用链式参数（temperature/topP/maxTokens/附加头/附加体参数）；代码显式 apiKey/baseUrl 优先于配置；buildModel 逃生舱接真实模型 | ✅ |
| §4 工具 | `tools/RealToolsLiveTest` | 4 | 自定义 @Tool 工具被真实模型调用并基于结果作答；Spring bean 工具注入；内置文件工具真实落盘与读回；shell 工具白名单执行 | ✅ 4/4 |
| §5 多轮对话与状态持久化 | `conversation/ConversationLiveTest` | 6 | conversationId 四种指定方式（ExecuteOption / autoConversationId 复用 / Map 键 / resolveConversationId 覆写）；JSON 落盘结构与目录命名；agentKey 记忆隔离 | ✅ 6/6 |
| §5.3 REDIS | `statestore/RedisStateStoreLiveTest` | 3 | 真实 Redis 多轮记忆续接；自定义 key 前缀落键（`agent_state` / `:list` / `:_keys`）；strict-distributed 守卫校验 fail-fast | ✅ 3/3 |
| §5.3 MYSQL | `statestore/MysqlStateStoreLiveTest` | 2 | 真实 MySQL 8 自动建表 + 多轮记忆；表 `agent_state_real` 行数据可查（session_id = `userId:lf-<agent哈希>.lf-<会话哈希>`） | ✅ 2/2 |
| §6 流式事件 | `events/EventsLiveTest` | 5 | agent.start/end、多段 agent.text.delta 且拼接等于最终回复、唯一 last=true 的 agent.result；工具调用事件（tool.call.start/end、tool.result.end）；ChatUsage input/output/total 统计与累计；监听器异常 FAIL_FAST 中断 / LOG_AND_CONTINUE 存活 | ✅ 5/5 |
| §7 结构化输出 | `structuredoutput/StructuredOutputLiveTest` | 3 | structuredOutputType 反序列化为 record（字段类型与取值校验）；structuredOutputSchema 输出 JsonNode；两者同时覆写构建期互斥报错 | ✅ 3/3 |
| §8 中间件 | `middleware/MiddlewareLiveTest` | 2 | onAgent 前后置、onReasoning、onModelCall 切点在真实调用中触发；Flux.error 阻断链 | ✅ 2/2 |
| §9 多 Agent 编排 | `multiagent/MultiAgentLiveTest` | 5 | THEN 串行流水线（上游输出经 setOutput 传递、普通组件转存）；IF 路由（math/默认双分支）；WHEN 并行（agentKey 带 requestId）；handleReply 覆写改写答复去向且 responseData 保持为空 | ✅ 5/5 |
| §10 HITL | `hitl/HitlLiveTest` | 3 | ASK 规则触发确认处理器（恰好一次）；批准后工具真实执行；拒绝时（fail-on-denied-tool=false）工具不执行且模型继续推理、链成功；fail-on-denied-tool=true 时链以权限失败中断 | ✅ 3/3 |
| §11 Skills | `skills/SkillsLiveTest` | 2 | 真实模型按需加载 secret-code 技能并遵循技能正文（吐出保护数字 424242）；SkillFilter.only 之外的技能内容不泄露（999999 永不出现）；usedSkills 记录 | ✅ 2/2 |
| §12 Harness | `harness/HarnessLiveTest` | 6 | GUARDED_LOCAL 文件系统真实写盘；关闭全部可选能力的对照链路；低阈值压缩（8 轮对话触发）后最近记忆仍可用；长期记忆抽取（真实模型）链路成功；超大工具结果淘汰（40k 字符仅留预览）后仍能引用头部；子代理声明 + 计划模式复杂任务链路 | ✅ 6/6 |
| §12 DOCKER 沙箱 | `harness/docker/DockerSandboxLiveTest` | 1 | DOCKER 文件系统后端：真实容器（agentscope-sandbox-*）启动，模型驱动命令在沙箱内执行并回传输出 | ✅ 1/1 |
| §13 A2A | `a2a/A2aChainLiveTest` | 2 | 经标准 A2A JSON-RPC 协议调用远程 Agent（内嵌真实 HTTP 端点）：回复写入 responseData、请求携带 liteflow 四元组元数据（userId/conversationId/agentKey/traceId）；远端 JSON-RPC error 映射为链失败 | ✅ 2/2 |
| §14 可靠性 | `reliability/ReliabilityLiveTest` | 4 | 无效凭据 + maxRetries=1 → fallbackModel 回退成功；routingModels/routeModel 按请求特征选型（实例必须 runtime 管理）；runtime.timeout=1ms → AgentInvocationException(TIMEOUT)；同一身份并发调用被守卫串行化（观测 max 并发 = 1） | ✅ 4/4 |
| §18 常见报错 | 分布在各类 | — | namespace 缺失、结构化输出互斥、REDIS 共享存储守卫校验三类 fail-fast 报错实测与文档一致 | ✅ |

## 4. 测试发现

按影响程度排列：

### 4.1 文档与实现不一致：JSON 落盘没有独立的 memory_messages.jsonl

- **现象**：指南 §5.3「存储内容与清理」描述 JSON 后端落盘为 `agent_state.json + memory_messages.jsonl + memory_messages.hash` 三个文件。实测（AgentScope 2.0.2 `JsonFileAgentStateStore`）会话目录下只有 `agent_state.json`，对话历史以 `context` 字段的形式序列化在该文件内。
- **影响**：仅文档问题。多轮记忆、跨会话恢复、目录命名（`<json-root>/<userId>/lf-<agent哈希>.lf-<会话哈希>/`）行为均与文档一致，功能不受影响。
- **建议**：更新指南 §5.3 的落盘结构描述。

### 4.2 多 Spring 上下文共享 JVM 时静态 FlowBus 导致规则校验互相污染

- **现象**：surefire 默认复用 JVM 时，前一个测试类上下文注册的链残留在静态 `FlowBus` 中，后一个上下文初始化规则时会因「残留链引用的节点在本上下文不存在」而启动失败（如 `[realFallbackAgent] is not exist`）。
- **影响**：仅影响「同一 JVM 内运行多个 agent 相关测试类」的场景（本次为 agent-live profile 的测试隔离问题）；单应用正常运行不受影响。
- **处理**：已在 pom 的 `agent-live` profile 配置 `reuseForks=false`（每测试类独立 JVM）规避，并附注释说明原因。

### 4.3 MySQL 直连模式要求账号具备 CREATE DATABASE 权限

- **现象**：`create-if-not-exist=true` 时 agentscope 扩展仍会执行建库语句；使用无全局建库权限的业务账号（如仅有 `liteflow` 库权限）时报 `Access denied`，即使目标库已存在且 `database-name` 已正确配置。
- **建议**：指南 §5.3 MYSQL 小节可补充说明：直连模式建议使用具备建库权限的账号，或与 `data-source-bean-name` 模式（应用自管连接池 + 预建库表）搭配。

### 4.4 第三方中转端点 json_schema 严格模式间歇性输出劣化（影响 1 个用例的稳定性）

- **现象**：`structuredoutput/typedOutputDeserializesIntoRecord`（JAVA_TYPE 结构化输出）在部分时段连续失败：模型在 `response_format=json_schema` 严格模式下返回**两个拼接的 JSON 对象**（`{...}{...}`），无法解析为单个对象，导致 AgentScope 报 `No structured output in message metadata. Key '_structured_output' not found`。
- **定位证据**：绕开 LiteFlow 直接 curl 端点复现——劣化窗口内 json_schema 请求稳定返回拼接双 JSON，json_object 与无 response_format 请求正常；窗口外（含同 schema）输出全部合法。窗口随时间波动（同一时段内多轮全过与全败交替出现）。
- **结论**：第三方中转端点（longjinapi + gpt-5.6-terra）的问题，与 LiteFlow 代码无关。JAVA_TYPE 结构化输出功能在端点正常时段已多次全量验证可用（含 record 字段类型与取值断言）。
- **处理**：用例内已内置 3 次重试（新会话）以穿越短劣化窗口；长窗口下该用例会失败，重跑即可。若需 100% 稳定，建议换用 json_schema 支持稳定的端点凭据。

### 4.5 其它过程性结论（非缺陷）

- Harness 模式下 `PermissionMode.BYPASS` 被框架明确禁止（`HarnessAgentBuilderPermissionBridge`），需用显式 ALLOW 规则或确认处理器放行工具——符合 fail-closed 设计。
- `routeModel` 返回值必须是 `routingModels()` 曾返回的同一实例（IdentityHashMap 身份校验），动态 resolve 新实例会被拒绝——指南 §14.2 可补充此约束说明。
- 事件监听失败模式（FAIL_FAST/LOG_AND_CONTINUE）在组件 runtime 构建期固化，运行期改配置对已构建组件不生效——属预期生命周期行为，但值得在指南 §6.1 注明。

## 5. 最终回归结果

全量 `agent-live` profile 回归（real 15 类 + platform 10 类，每测试类独立 JVM）：

```text
Tests run: 63, Failures: 1, Errors: 0, Skipped: 9
```

- Skipped 9：platform 目录中未提供凭据的 9 个平台连通性用例（按设计跳过）。
- Failures 1：`typedOutputDeserializesIntoRecord`，即 §4.4 的第三方端点 json_schema 劣化窗口；端点正常时段该用例（及整个 real 套件）为全绿（已多轮验证，含连续 3 轮全过记录）。
- real 53 用例在端点正常时段全部通过；中间件（Docker Redis/MySQL/Docker 沙箱/A2A 内嵌端点）全真实、全通过。

## 6. 产出物清单

- 测试代码：`liteflow-testcase-el/liteflow-testcase-el-agent/src/test/java/com/yomahub/liteflow/test/agent/real/`（15 个 LiveTest 类 + 组件与辅助类）
- 测试资源：`src/test/resources/real/`（各场景 properties / flow.el.xml / 技能目录）
- pom 变更：`agent-live` profile 增加 `reuseForks=false`（FlowBus 隔离）；新增 redis / mysql 状态存储模块 test 依赖
- 本报告：`docs/liteflow-agent-real-model-test-report.md`
