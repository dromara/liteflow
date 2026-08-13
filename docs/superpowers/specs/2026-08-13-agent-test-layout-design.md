# AgentScope 2 测试布局规范化设计

## 背景

AgentScope 2 升级基线为提交 `2655a16b13d7fd95c48575c979cb5b3f45b84b7b`。该基线中，
`liteflow-react-agent-*` 生产模块下没有测试源码。当前分支在这些模块下共有 59 个 Java
测试源码与夹具，它们全部由本次 AgentScope 2 升级新增，因此全部属于本轮规范性迁移范围。

LiteFlow 的测试约定是把测试用例统一放在 `liteflow-testcase-el` 大模块中。当前布局把
AgentScope 2 的单元测试、契约测试、生命周期测试和测试夹具放进生产模块，违反该约定。

## 目标

把本次 AgentScope 2 升级新增的全部测试源码和测试夹具迁移到：

```text
liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test
```

迁移后应满足：

1. `liteflow-react-agent-*` 生产模块中不存在任何 `src/test` 文件；
2. 现有测试语义、覆盖范围、默认离线边界和显式 profile 行为保持不变；
3. 测试类保留原 Java package，以继续验证 package-private 契约；
4. 不为迁移测试而扩大生产 API；
5. JDK 17 下相关全量测试与 package 成功；
6. AgentScope 依赖继续全部收敛到 2.0.2，无 aggregate、1.x 或 core 污染。

## 范围

### 本轮迁移

迁移当前位于下列目录的全部 Java 测试类和辅助夹具：

| 来源模块 | 当前 Java 文件数 | 迁移内容 |
|---|---:|---|
| `liteflow-react-agent-core` | 30 | 组件、身份、事件、guard、HITL、middleware、model、runtime、skill、state、tool 测试与夹具 |
| `liteflow-react-agent-openai` | 3 | OpenAI、兼容模型及 first-party provider 契约 |
| `liteflow-react-agent-anthropic` | 2 | Anthropic spec 与 runtime ownership 契约 |
| `liteflow-react-agent-gemini` | 2 | Gemini spec 与 lifecycle 契约 |
| `liteflow-react-agent-dashscope` | 1 | DashScope spec 契约 |
| `liteflow-react-agent-harness` | 15 | 配置、component、filesystem、permission、sandbox、state、capability 与测试夹具 |
| `liteflow-react-agent-a2a` | 6 | client、component、public API、protocol adapter 与 server lifecycle 契约 |

合计 59 个 Java 文件。当前这些生产模块没有非 Java 测试资源需要迁移。

### 本轮不处理

以下内容不在本轮自动迁移范围内：

- AgentScope 2 升级前已存在、且与本次升级无关的仓库测试；
- 其他生产模块中不位于 `liteflow-testcase-el` 的历史测试；
- 测试语义重写、生产 API 重构或无关依赖整理。

完成本轮后，应单独审计整个仓库的非 `liteflow-testcase-el` 测试，并把清单、来源、建议与
风险提交给用户确认。未获得确认前不得迁移这些历史测试。

## 布局设计

### 文件系统布局

所有迁入文件位于：

```text
liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/<原 package 路径>
```

例如：

```text
liteflow-react-agent/liteflow-react-agent-core/src/test/java/
  com/yomahub/liteflow/agent/component/AbstractAgentComponentTest.java
```

迁移为：

```text
liteflow-testcase-el/liteflow-testcase-el-react-agent/src/test/java/
  com/yomahub/liteflow/agent/component/AbstractAgentComponentTest.java
```

文件物理位置改变，但 `package com.yomahub.liteflow.agent.component;` 保持不变。

### package-private 契约

保留原 package 是本设计的关键约束。Core、Provider、Harness 与 A2A 的部分测试会访问
package-private builder bridge、factory seam 或测试辅助类型。迁移不能通过把这些类型改成
`public` 来规避编译问题。

如果 testcase 模块缺少某个 package-private 测试入口，应优先采用以下顺序处理：

1. 保留原 package，并直接从 testcase 模块编译；
2. 把纯测试夹具一并迁入相同 package；
3. 仅当 Java／Maven 模块边界确实阻止访问时，重新设计为生产行为的公开可观察断言；
4. 不得新增只为测试服务的 public API。

## Maven 设计

### testcase 模块

`liteflow-testcase-el-react-agent` 继续作为全部 AgentScope 2 测试的唯一执行模块。其测试
classpath 应显式依赖以下 LiteFlow 模块：

- `liteflow-react-agent-core`；
- 四个 Provider 模块；
- `liteflow-react-agent-harness`；
- `liteflow-react-agent-a2a`。

只在测试编译或测试执行中需要的第三方依赖，应以 `test` scope 放入 testcase POM。不得
通过引入 AgentScope aggregate artifact 简化 classpath。

### 生产模块

测试迁出后，逐个清理 `liteflow-react-agent-*` POM 中仅供原模块测试使用的 JUnit、测试
夹具依赖或 Surefire 配置。生产编译、运行依赖和 optional 边界不得改变。

迁移完成后，生产模块自身运行 `mvn test` 可以显示“无测试”，但其测试契约必须由 testcase
模块统一执行。

## 测试迁移顺序

迁移按依赖从低到高分四批完成，每批都必须有独立的 RED／GREEN 证据：

1. **Core 批次**：迁移 27 个测试类及 3 个测试夹具；
2. **Provider 批次**：迁移 OpenAI、Anthropic、Gemini、DashScope 的 8 个测试源码；
3. **Harness 批次**：迁移 15 个测试源码与 fake sandbox／snapshot 夹具；
4. **A2A 批次**：迁移 6 个 client／server 测试源码。

每批迁移后，应在 testcase 模块中运行对应测试类，并确认生产模块的原文件已经删除。

## 结构契约

在 testcase 模块新增结构契约测试，检查仓库中：

```text
liteflow-react-agent/liteflow-react-agent-*/src/test/**
```

不得存在文件。该测试在迁移前必须因现有 59 个文件而失败，迁移后转为通过。契约测试不得
依赖当前工作目录的偶然值，应从 Maven module base directory 或稳定的仓库根定位路径。

同时保留 shell 静态扫描作为构建验收门禁，防止仅因契约测试定位错误而出现假绿。

## Profile 与外部环境边界

现有边界保持不变：

- 默认测试完全离线，不访问真实 Provider、网络或 Docker；
- `agent-live` profile 才发现真实 Provider 测试；
- Docker IT 只由已有显式 profile 运行；
- 未经授权或环境未验证时，Docker、live Provider 和真实 A2A transport 标记为“未执行”，
  不得伪装为通过；
- 默认测试不得用 skip 或 assumption 掩盖核心、Provider builder、Harness 或 A2A 覆盖。

## 验收标准

迁移完成后至少执行以下验证：

1. 结构契约测试通过；
2. `find liteflow-react-agent -path '*/src/test/*' -type f` 无输出；
3. testcase 模块定向测试覆盖原 59 个测试源码与夹具所承载的全部测试契约；
4. testcase 默认离线完整测试通过，零失败、错误或跳过；
5. Zulu JDK 17 下相关 16 模块 `package` 成功；
6. Surefire 汇总测试数不低于迁移前的 554 项；
7. dependency tree 中 AgentScope 全部为 2.0.2，无 `io.agentscope:agentscope` aggregate 或 1.x；
8. core 不传递 Provider、Harness 或 A2A 依赖；
9. `git diff --check` 和测试绕过扫描通过；
10. 独立代码审查无 Critical／Important。

## 失败处理

- 若迁移造成 package-private 编译失败，先验证 Java package 与 testcase classpath，不扩大生产
  可见性；
- 若测试依赖了原模块的 Maven base directory，应把路径定位改为显式仓库／fixture 根，并用
  迁移前后行为测试证明等价；
- 若某个测试只能依赖原模块生命周期才能运行，应把该生命周期显式组装在 testcase 中，
  不得把测试移回生产模块；
- 若发现迁移前已存在的独立生产缺陷，先形成自然 RED 并单独报告，不把行为修复混入机械
  文件迁移。

## 后续历史测试审计

本轮完成后生成一份只读清单，列出整个仓库中仍存在于 `liteflow-testcase-el` 之外的测试，
至少包含：模块、文件数、最初提交时间、是否与 AgentScope 2 有关、迁移依赖和建议优先级。
该清单提交给用户确认后，才决定是否开启下一轮历史测试规范化。
