# AgentScope 2 测试集中迁移报告

## 结论

AgentScope 2 升级新增的 63 个测试源码与夹具已全部从生产模块迁入
`liteflow-testcase-el` 大模块。迁移保持原 Java package、默认离线边界和既有测试语义，
没有为测试扩大生产 API。

最终布局按运行容器隔离为：

- `liteflow-testcase-el-agent-core`：31 个 Core／基础配置文件和 1 个 Solon
  ServiceLoader 隔离测试；
- `liteflow-testcase-el-agent-harness`：15 个 Harness 测试与夹具；
- `liteflow-testcase-el-agent`：8 个 Provider 文件和 6 个 A2A 文件；
- `liteflow-testcase-el-springboot`：Spring Boot 3 配置绑定测试；
- `liteflow-testcase-el-springboot4`：Spring Boot 4 配置绑定测试。

Solon 测试先迁入完整 Solon testcase 套件，但新鲜全量验收证明前序测试会启动
`AppContext`，破坏该契约原有的 `Solon.context() == null` 前提。最终将它放入无容器
Core testcase，并通过 test-scope `liteflow-solon-plugin` 保留原契约。

## TDD 与分批迁移

结构契约在迁移前自然 RED，准确报告 63 个禁止位置文件：

- `liteflow-agent-*`：59 个；
- `liteflow-core`：`AgentConfigV2Test`；
- Spring Boot 3／4：各 1 个 `AgentPropertyBindingTest`；
- Solon：`SolonCmpAroundAspectTest`。

迁移后的 focused 结果如下：

| 批次 | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| Core＋基础配置＋Solon 隔离 | 190 | 0 | 0 | 0 |
| Harness | 187 | 0 | 0 | 0 |
| Provider＋A2A＋现有 Agent 纵切 | 179 | 0 | 0 | 0 |
| Spring Boot 3 绑定 | 5 | 0 | 0 | 0 |
| Spring Boot 4 绑定 | 4 | 0 | 0 | 0 |
| 合计 | 565 | 0 | 0 | 0 |

其中源文件迁移均由 Git 识别为 100％ rename；Solon 文件的第二次 rename 是容器隔离修正，
不增加源文件总数。

## JDK 17 完整验收

环境：Zulu OpenJDK 17.0.18。

执行了：

```bash
mvn clean package \
  -pl liteflow-agent,\
liteflow-testcase-el/liteflow-testcase-el-agent-core,\
liteflow-testcase-el/liteflow-testcase-el-agent-harness,\
liteflow-testcase-el/liteflow-testcase-el-agent,\
liteflow-testcase-el/liteflow-testcase-el-springboot,\
liteflow-testcase-el/liteflow-testcase-el-springboot4,\
liteflow-testcase-el/liteflow-testcase-el-solon \
  -am -DskipTests=false -DskipITs
```

结果：23／23 个 Reactor 模块 `BUILD SUCCESS`。

新鲜 Surefire XML 汇总：

| testcase 子模块 | Suites | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|---:|
| Agent Core | 29 | 190 | 0 | 0 | 0 |
| Agent Harness | 12 | 187 | 0 | 0 | 0 |
| Agent Spring／Provider／A2A | 42 | 179 | 0 | 0 | 0 |
| Spring Boot 3 | 85 | 371 | 0 | 0 | 1 |
| Spring Boot 4 | 82 | 359 | 0 | 0 | 1 |
| Solon | 56 | 240 | 0 | 0 | 1 |
| 合计 | 306 | 1526 | 0 | 0 | 3 |

完整套件的 3 个 skip 全部来自三个框架 testcase 中既有的
`AbsoluteConfigPathELSpringbootTest` Windows 条件方法。三个文件最早均可追溯到
2021-04-04 的提交 `0a1e714f6`，早于 AgentScope 2 升级基线；本轮未新增、修改或通过排除
制造这些 skip。上表之外，迁入契约的 565 项定向测试严格为 0 skip。

## 依赖与结构门禁

- AgentScope 依赖全部为 2.0.2；
- 未引入 `io.agentscope:agentscope` aggregate 或 1.x；
- Agent Core 仅依赖 `agentscope-core`，没有 Provider、Harness 或 A2A 反向污染；
- A2A client／server 只在 A2A 生产边界和 testcase test classpath 出现；
- `find liteflow-agent -path '*/src/test/*' -type f` 无输出；
- 四个升级相关的原生产模块测试路径均不存在；
- 永久 `AgentTestLayoutContractTest` 已通过；
- 迁入测试没有新增 `@Disabled`、assumption 或测试排除；
- `git diff --check` 通过。

## 外部环境边界

默认验证没有调用真实 Provider、Docker daemon、网络或真实 A2A transport。Docker IT 和
live Provider 仍只属于原有显式 profile；本轮未获授权，因此未执行，也没有将其表述为通过。

## 独立审查

独立审查 verdict：READY；Critical 0、Important 0。Reviewer 唯一提出的 Minor 是计划与
设计中残留旧 Solon 目标路径和遗漏两个新 testcase 子模块的最终命令；已同步为最终布局。
