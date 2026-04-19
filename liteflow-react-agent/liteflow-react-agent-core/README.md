# liteflow-react-agent-core

## 快速上手

### 1. 添加依赖（选择至少一个平台模块）

```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-react-agent-openai</artifactId>
    <version>${liteflow.version}</version>
</dependency>
```

### 2. 配置

```yaml
liteflow:
  agent:
    workspace:
      root: /var/lib/liteflow/agent-workspaces
    shell:
      mode: whitelist
      whitelist: [ls, cat, grep]
    openai-compatible:
      deepseek:
        api-key: ${DEEPSEEK_API_KEY}
        base-url: https://api.deepseek.com/v1
```

### 3. 定义 Agent

```java
@LiteflowComponent("reviewAgent")
public class ReviewAgent extends ReActAgentComponent {
    @Override protected Model buildModel(ReActAgentContext ctx) {
        return OpenAICompatiblePresets.deepseek(
            agentConfig().getOpenaiCompatible().get("deepseek").getApiKey(),
            "deepseek-chat"
        );
    }
    @Override protected String systemPrompt(ReActAgentContext ctx) { return "你是审核专家"; }
    @Override protected String userPrompt(ReActAgentContext ctx) {
        return ctx.getSlot().getRequestData(String.class);
    }
}
```

### 4. EL 编排

```xml
<chain name="reviewChain">
    THEN(prepare, reviewAgent, notify);
</chain>
```

## 核心概念

- **Session**：由 `resolveSessionId` 决定；默认 `slot.getRequestId()`（一次性）。覆写后可复用 memory 与 workspace 实现多轮对话。
- **Workspace**：每 session 一个目录，在 `liteflow.agent.workspace.root` 之下。内置 `WorkspaceFileTools` 强制路径围栏。
- **Shell**：`ManagedShellCommandTool` 按 `liteflow.agent.shell.mode` 做 whitelist/blacklist/disabled 校验，首 token 不在策略内即拒绝。

## 可选覆写

| 方法 | 默认行为 | 说明 |
|------|----------|------|
| `tools(ctx)` | 空列表 | 注册自定义 agentscope @Tool 对象 |
| `resolveSessionId(slot)` | `slot.getRequestId()` | 覆写以实现多轮对话 |
| `maxIterations()` | 配置文件默认值 (15) | ReAct 最大推理轮数 |
| `enableShellTool()` | true | 是否启用受管 shell 工具 |
| `enableWorkspaceFileTools()` | true | 是否启用受管文件工具 |
| `hooks(ctx)` | 空列表 | agentscope Hook 列表 |
| `handleReply(reply, ctx)` | 写入 slot.responseData | 自定义回复处理 |