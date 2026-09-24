# Jev 决策组件

本模块把 TypeSafe 的 Jev 类型化决策协议接入 LiteFlow 的分支判断，需要 JDK 17+，依赖 `liteflow-core`，不要求初始化 AgentScope、会话存储或其他聊天模型。现有两个组件：

- `JevSwitchComponent` 继承 `NodeSwitchComponent`，通过 Choice 接口选择目标 ID，直接配合 `SWITCH(...).to(...).DEFAULT(...)` 使用。
- `JevBooleanComponent` 继承 `NodeBooleanComponent`，通过 Noul 接口做是非判断，直接配合 `IF(...)` 及 `AND` / `OR` / `NOT` 使用。

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-agent-jev</artifactId>
    <version>2.16.3</version>
</dependency>
```

配置示例：

```yaml
liteflow:
  agent:
    jev:
      api-key: ${JEV_API_KEY}
      base-url: https://api.typesafe.ai/v1
      model: jev-1.13.0
      timeout: 3s
      min-confidence: 0.6
      noul-threshold: 0.5
```

除 `api-key` 外，上述值也是默认值。`base-url` 是包含 `/v1` 的 API 根地址，支持末尾斜杠和网关路径，客户端追加 `/systemone`。`timeout` 限制等待完整 HTTP 响应的时间，包括响应体。配置仅在使用 Jev 时校验，不会要求普通组件或其他 Agent 提供 Jev 凭据。

## 本地自托管 laya

[laya](https://github.com/NandhaKishorM/laya) 是开源的本地决策引擎（Apache 2.0），其 `laya-serve` 服务实现了同一套 `POST /v1/systemone` 协议，两个组件可以不经修改切换到本地运行的小模型：不需要真实 API Key、不产生调用费用、数据不出本机。首次加载模型时它会自动从 Hugging Face 下载 checkpoint 到本地缓存，之后离线可用。

```bash
pip install "laya[serve]"
laya-serve    # 默认监听 0.0.0.0:8000，启动时下载并预热 checkpoint
```

```yaml
liteflow:
  agent:
    jev:
      provider: laya
```

`provider: laya` 下 `base-url` 默认为 `http://127.0.0.1:8000/v1`，`model` 默认为 `auto`——它不是具体 checkpoint 名，表示由 laya 的路由器按文本语言自动选择（中文会进入多语言 checkpoint）；也可显式填 `english`、`multilingual` 或 `typed-decisions` 固定某个 checkpoint。`api-key` 仅当服务端设置了 `LAYA_API_KEY` 时才需要配置，且值必须相同；其余情况留空即可，客户端不会发送 Authorization 头。

laya-serve 默认在启动时预热模型（`LAYA_PRELOAD=1`）。若关闭预热，第一个请求会触发模型下载与加载，耗时远超 `timeout` 默认的 3 秒，请保持预热开启或相应调大超时。

## 选择组件（SWITCH）

业务组件实现 `state()`、`instructions()` 和 `choices()`：

```java
@LiteflowComponent("supportRouter")
public class SupportRouter extends JevSwitchComponent {
    @Override
    protected Object state() {
        return getContextBean(SupportContext.class).getMessage();
    }

    @Override
    protected String instructions() {
        return "根据客户当前诉求选择处理流程，注意否定和意图变化。";
    }

    @Override
    protected Map<String, String> choices() {
        return Map.of("refundChain", "客户希望退款", "exchangeChain", "客户希望换货");
    }

    @Override
    protected void onDecision(JevChoiceResult result) {
        getContextBean(SupportContext.class).setDecision(result);
    }
}
```

```text
SWITCH(supportRouter).to(refundChain, exchangeChain).DEFAULT(manualChain);
```

`state()` 可返回文本、可序列化为 JSON 对象的业务数据或数组。请只提供判断所需字段。`choices()` 的 key 为目标组件或子链的 ID，value 为非空业务描述；最多 254 个候选，剩余一个选项由框架提供“均不适用”。候选 ID 必须在当前 `.to(...)` 中唯一，第一版不接受 `tag:...` 标签选择语法，也不接受同 ID 的多个目标。

执行行为：

- 正常命中且置信度达到阈值，返回所选 ID。
- 低置信度或模型选择保留项 `JevSwitchComponent.NO_MATCH`，返回空字符串，触发现有 `DEFAULT`。未配置 `DEFAULT` 时沿用 LiteFlow 的无匹配目标异常。
- 鉴权失败、限流、超时或非法响应抛出 `JevInvocationException`，不会伪装成默认业务分支。HTTP 错误可读取 `getStatusCode()`。客户端不自动重试，可由业务按异常和预算决定处理方式。
- 中断会取消等待并保留线程中断标记。

`minConfidence()` 可在组件中覆盖，全局默认来自 `liteflow.agent.jev.min-confidence`。该值需要使用真实业务样本校准，置信度不等于单次判断的正确率。

`onDecision()` 在当前流程线程执行，正常、低置信度和均不适用的有效结果都会进入回调。`JevChoiceResult` 保留实际模型版本、原始选项、置信度和不可变概率分布。请把结果放入本次流程的上下文；组件实例会被并发复用，不要保存到实例字段。回调抛出的异常会阻止目标执行。

## 布尔判断组件（IF）

业务组件实现 `state()` 和 `instructions()`，提出一个是非问题：

```java
@LiteflowComponent("humanEscalation")
public class HumanEscalation extends JevBooleanComponent {
    @Override
    protected Object state() {
        return getContextBean(SupportContext.class).getMessage();
    }

    @Override
    protected String instructions() {
        return "客户是否要求人工客服";
    }

    @Override
    protected void onDecision(JevNoulResult result) {
        getContextBean(SupportContext.class).setEscalationProbability(result.probability());
    }
}
```

```text
IF(humanEscalation, manualChain, botChain);
```

Noul 回答是“是”的概率，没有独立的置信度；概率达到阈值（含边界）走 true 分支，否则走 false 分支。`probabilityThreshold()` 可在组件中覆盖，全局默认来自 `liteflow.agent.jev.noul-threshold`（默认 0.5）。阈值取决于误判代价：误报代价高就调高，漏报代价高就调低，需要用真实业务样本校准。布尔组件可以进入 `AND` / `OR` / `NOT` 组合；需要中间地带转人工时，可在 `onDecision()` 中读取原始概率自行分流。

执行行为与选择组件一致：鉴权失败、限流、超时或非法响应抛出 `JevInvocationException`，不会伪装成 false；`onDecision()` 对每个有效结果回调；组件实例并发复用，状态只放上下文。第一版不发送 Noul 的可选 `criteria`（true/false 描述），仅靠 `instructions()` 表达边界。

离线测试位于 `liteflow-testcase-el/liteflow-testcase-el-agent-jev`，通过本地 HTTP 模拟服务验证请求协议、异常、超时与实际分支执行，不调用真实模型：

```bash
mvn -pl liteflow-testcase-el/liteflow-testcase-el-agent-jev -am test -DskipTests=false
```

官方协议：[Choice](https://docs.typesafe.ai/primitives/choice)、[Noul](https://docs.typesafe.ai/primitives/noul)、[HTTP API](https://docs.typesafe.ai/api)、[模型与语言支持](https://docs.typesafe.ai/models)。真实中文识别效果需要另行使用业务样本验证。
