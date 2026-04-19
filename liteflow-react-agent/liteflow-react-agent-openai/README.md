# liteflow-react-agent-openai

OpenAI 及 OpenAI 兼容协议厂商支持。

## 使用

```java
// 标准 OpenAI
OpenAIChatModel m1 = OpenAIModelFactory.openai(apiKey, "gpt-4o-mini");

// DeepSeek
OpenAIChatModel m2 = OpenAICompatiblePresets.deepseek(apiKey, "deepseek-chat");

// Kimi (Moonshot)
OpenAIChatModel m3 = OpenAICompatiblePresets.kimi(apiKey, "moonshot-v1-8k");

// GLM (智谱)
OpenAIChatModel m4 = OpenAICompatiblePresets.glm(apiKey, "glm-4");

// MiniMax
OpenAIChatModel m5 = OpenAICompatiblePresets.minimax(apiKey, "abab6.5s-chat");

// 自定义 OpenAI 兼容端点
OpenAIChatModel m6 = OpenAIModelFactory.custom(apiKey, "https://your.own/v1", "your-model");
```