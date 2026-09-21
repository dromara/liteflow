# Jev 选择组件

`JevSwitchComponent` 继承 `NodeSwitchComponent`，通过 TypeSafe 的 Choice 接口选择目标 ID，直接配合 `SWITCH(...).to(...).DEFAULT(...)` 使用。需要 JDK 17+，依赖 `liteflow-core`，不要求初始化 AgentScope、会话存储或其他聊天模型。

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-agent-jev</artifactId>
    <version>2.16.2</version>
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
```

除 `api-key` 外，上述值也是默认值。`base-url` 是包含 `/v1` 的 API 根地址，支持末尾斜杠和网关路径，客户端追加 `/systemone`。`timeout` 限制等待完整 HTTP 响应的时间，包括响应体。配置仅在使用 Jev 时校验，不会要求普通组件或其他 Agent 提供 Jev 凭据。

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

离线测试位于 `liteflow-testcase-el/liteflow-testcase-el-agent-jev`，通过本地 HTTP 模拟服务验证请求协议、异常、超时与实际分支执行，不调用真实模型：

```bash
mvn -pl liteflow-testcase-el/liteflow-testcase-el-agent-jev -am test -DskipTests=false
```

官方协议：[Choice](https://docs.typesafe.ai/primitives/choice)、[HTTP API](https://docs.typesafe.ai/api)、[模型与语言支持](https://docs.typesafe.ai/models)。真实中文识别效果需要另行使用业务样本验证。
