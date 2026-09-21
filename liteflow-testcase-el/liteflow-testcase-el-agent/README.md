# Agent 测试标准

Agent testcase 不需要真实 API key、模型账户、Docker daemon、MySQL 或 Redis 服务。外部边界使用脚本模型、mock 客户端或仅监听 `127.0.0.1` 随机端口的 mock 服务。LiteFlow 组件、AgentScope 运行时、Provider SDK、规则解析和序列化逻辑仍执行真实代码。数据库语义测试使用隔离的 H2，文件持久化测试使用临时目录。

四个模块默认执行测试，覆盖 `liteflow-agent` 下全部九个生产模块。命令行显式传入 `-DskipTests` 仍可跳过。已移除 `agent-live`、`agent-docker-it` 和真实凭据读取工具；原 `real/` 测试中的功能场景由下列确定性用例承接。

| 能力／原场景 | 默认执行的测试 |
| --- | --- |
| 基础链、参数、凭据优先级、组件扩展 | `BasicChainTest`、`BuildModelEscapeTest`、`AgentBuilderConfigurationTest`、各 `*SpecTest` |
| 10 个 Provider 入口 | `ProviderProtocolTest` 的 40 个参数化用例，验证文本、usage、SSE、工具 schema／参数和 HTTP 401 |
| THEN、IF、WHEN、多 Agent、输出处理 | `MultiAgentChainTest`、`IfRoutingAgentTest`、`WhenParallelAgentTest`、`HandleReplyOverrideTest` |
| 多轮记忆、会话隔离、状态、删除并发 | `MultiTurnMemoryTest`、`RuntimeContextIsolationTest`、`AgentConversationServiceTest`、`HarnessConversationServiceTest`、`AgentConversationProcessTest` |
| 结构化输出、重试、降级、超时、模型路由 | `AgentStructuredOutputTest`、`StructuredOutputChainTest`、`AgentRetryFallbackTest`、`AgentCallExecutorTest`、`ModelRoutingMiddlewareTest` |
| 工具、Spring Bean、workspace、MCP、A2A | `CustomToolRegistrationTest`、`SpringBeanToolInjectionTest`、`WorkspaceToolsFeatureTest`、`McpTransportIntegrationTest`、`A2aChainProtocolTest` |
| HITL、权限、事件、usage、中间件 | `HitlChainTest`、`HarnessPermissionHitlTest`、`TypedAgentEventsTest`、`ChatUsageTest`、`MiddlewareIntegrationTest` |
| 技能、压缩、长期记忆、结果淘汰、计划、子代理 | `AgentSkillRepositoryIntegrationTest`、`HarnessSkillsIntegrationTest`、`HarnessCapabilitiesTest`、`AdaptiveCompactionMiddlewareTest` |
| 沙箱创建、复用、恢复、回收、失败重试、投影 | `SandboxLifecycleTest`、`SessionSandboxRegistryTest`、`CheckpointRetryTest`、`WorkspaceProjectionTest`，使用 `FakeSandboxClient` |
| MySQL／Redis 存储、CAS、分页、迁移、客户端所有权 | `MysqlConversationServiceTest`、`MysqlBeanAndSchemaTest`、`MysqlHarnessStorageTest`、`CatalogDataSourceTest`、`RedisConversationServiceTest`、`RedisStorageContractTest`、`RedisStorageConnectionTest` |
| 分布式锁、续租、失锁、中断、清理异常 | `MysqlInvocationGuardTest`、`RedisStorageContractTest`，mock JDBC／Redis 客户端 |
| JEV 选择、阈值、路由和异常 | `JevChoiceClientTest`、`JevSwitchComponentTest`，使用 `MockJevServer` |

## 执行

在仓库根目录使用 JDK 17 或更高版本：

```sh
mvn -pl liteflow-testcase-el/liteflow-testcase-el-agent-core,liteflow-testcase-el/liteflow-testcase-el-agent-harness,liteflow-testcase-el/liteflow-testcase-el-agent,liteflow-testcase-el/liteflow-testcase-el-agent-jev -am clean test
```

`clean` 会清除旧 live 测试的 class 文件和以前复制的本地凭据。历史本地 `src/test/resources/env.txt` 保持 Git 忽略，并从测试资源复制中排除；测试不会读取该文件或环境中的模型凭据。

`AgentTestLayoutContractTest` 会拒绝重新引入凭据环境读取、长格式 `sk-` 密钥、按环境启用／跳过测试、live profile、Surefire 测试筛选，以及默认跳过 Agent 测试的配置。它还会确认测试 classpath 中没有 `env.txt`。

## 覆盖率

```sh
mvn -Pagent-coverage -pl :liteflow-agent-test-coverage -am clean \
  org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent verify
```

HTML 报告位于 `liteflow-testcase-el/agent-coverage/target/site/jacoco-aggregate/index.html`，同目录提供 `jacoco.xml` 和 `jacoco.csv`。聚合报告的分母是九个 `liteflow-agent-*` 生产模块，不把 mock 或测试代码计入覆盖率。

验收要求是测试零失败、零错误、零跳过，主要功能同时覆盖正常和失败路径；各生产模块行覆盖率至少 80%，整体分支覆盖率至少 70%。覆盖率由聚合报告核对，Maven 当前不自动拦截百分比下降。mock 协议测试验证客户端契约，不依赖模型生成内容的随机性。

2026-09-21 在 JDK 21 上执行上述完整验证命令：808 个测试通过，失败、错误和跳过均为 0；聚合行覆盖率 87.88%，分支覆盖率 73.31%，九个生产模块各自行覆盖率均超过 80%。
