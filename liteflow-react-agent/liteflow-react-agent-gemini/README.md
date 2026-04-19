# liteflow-react-agent-gemini

Google Gemini 模型支持。

## 使用

```java
// 基础
GeminiChatModel m1 = GeminiModelFactory.of(apiKey, "gemini-3-flash-preview");

// 带 thinking level
GeminiChatModel m2 = GeminiModelFactory.of(apiKey, "gemini-3-flash-preview", "high");
```