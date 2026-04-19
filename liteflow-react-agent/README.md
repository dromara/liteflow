# liteflow-react-agent

为 LiteFlow 提供基于 agentscope-java 的 ReAct Agent 组件能力。通过继承 `ReActAgentComponent` 即可在 EL 规则中作为普通 node 编排。

## 子模块

| 模块 | 说明 |
|------|------|
| `liteflow-react-agent-core` | 抽象类、Session/Workspace 管理、内置受管工具 |
| `liteflow-react-agent-openai` | OpenAI + 兼容协议（DeepSeek/Kimi/GLM/MiniMax） |
| `liteflow-react-agent-anthropic` | Anthropic Claude |
| `liteflow-react-agent-gemini` | Google Gemini |
| `liteflow-react-agent-dashscope` | 阿里云 DashScope / Qwen |

## 配置

见 [core 模块 README](./liteflow-react-agent-core/README.md)。

## Java 版本

所有 react-agent 子模块要求 **Java 21+**（agentscope-java 要求）。LiteFlow 主工程仍然保持 Java 8 兼容。