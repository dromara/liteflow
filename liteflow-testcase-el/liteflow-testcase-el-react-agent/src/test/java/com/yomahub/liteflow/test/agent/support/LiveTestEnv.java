package com.yomahub.liteflow.test.agent.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一管理本测试模块需要的所有真实平台凭据环境变量与系统属性。
 *
 * <p>以避免分散硬编码：所有真实 apikey/baseUrl/model 都来自这里，缺失时由
 * 各测试用 {@code Assumptions.assumeTrue} 跳过。
 *
 * <p>查找顺序：环境变量 → JVM system property（便于 IDE -D 指定）→ classpath 下的 env.txt 文件。
 */
public final class LiveTestEnv {

    /* compatible-custom 功能测试用一组凭据 */
    public static final String COMPATIBLE_API_KEY = "LITEFLOW_AGENT_TEST_API_KEY";
    public static final String COMPATIBLE_BASE_URL = "LITEFLOW_AGENT_TEST_BASE_URL";
    public static final String COMPATIBLE_MODEL = "LITEFLOW_AGENT_TEST_MODEL";

    /* 头等平台 */
    public static final String OPENAI_API_KEY = "LITEFLOW_AGENT_TEST_OPENAI_API_KEY";
    public static final String OPENAI_BASE_URL = "LITEFLOW_AGENT_TEST_OPENAI_BASE_URL";
    public static final String OPENAI_MODEL = "LITEFLOW_AGENT_TEST_OPENAI_MODEL";

    public static final String ANTHROPIC_API_KEY = "LITEFLOW_AGENT_TEST_ANTHROPIC_API_KEY";
    public static final String ANTHROPIC_MODEL = "LITEFLOW_AGENT_TEST_ANTHROPIC_MODEL";

    public static final String GEMINI_API_KEY = "LITEFLOW_AGENT_TEST_GEMINI_API_KEY";
    public static final String GEMINI_MODEL = "LITEFLOW_AGENT_TEST_GEMINI_MODEL";

    public static final String DASHSCOPE_API_KEY = "LITEFLOW_AGENT_TEST_DASHSCOPE_API_KEY";
    public static final String DASHSCOPE_MODEL = "LITEFLOW_AGENT_TEST_DASHSCOPE_MODEL";

    /* OpenAI 兼容族预设 */
    public static final String DEEPSEEK_API_KEY = "LITEFLOW_AGENT_TEST_DEEPSEEK_API_KEY";
    public static final String DEEPSEEK_MODEL = "LITEFLOW_AGENT_TEST_DEEPSEEK_MODEL";

    public static final String KIMI_API_KEY = "LITEFLOW_AGENT_TEST_KIMI_API_KEY";
    public static final String KIMI_MODEL = "LITEFLOW_AGENT_TEST_KIMI_MODEL";

    public static final String GLM_API_KEY = "LITEFLOW_AGENT_TEST_GLM_API_KEY";
    public static final String GLM_MODEL = "LITEFLOW_AGENT_TEST_GLM_MODEL";

    public static final String MINIMAX_API_KEY = "LITEFLOW_AGENT_TEST_MINIMAX_API_KEY";
    public static final String MINIMAX_MODEL = "LITEFLOW_AGENT_TEST_MINIMAX_MODEL";

    /* Anthropic 兼容网关 */
    public static final String ANTHROPIC_GATEWAY_API_KEY = "LITEFLOW_AGENT_TEST_ANTHROPIC_GATEWAY_API_KEY";
    public static final String ANTHROPIC_GATEWAY_BASE_URL = "LITEFLOW_AGENT_TEST_ANTHROPIC_GATEWAY_BASE_URL";
    public static final String ANTHROPIC_GATEWAY_MODEL = "LITEFLOW_AGENT_TEST_ANTHROPIC_GATEWAY_MODEL";

    /** 从 classpath env.txt 加载的 key=value 映射，懒加载一次。 */
    private static volatile Map<String, String> envFileMap;

    private LiveTestEnv() {
    }

    public static String resolve(String key) {
        // 1) 环境变量
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        // 2) JVM system property
        String prop = System.getProperty(key);
        if (prop != null && !prop.isBlank()) {
            return prop.trim();
        }
        // 3) classpath 下的 env.txt
        String fromFile = getEnvFileMap().get(key);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        return "";
    }

    public static String resolveOrDefault(String key, String fallback) {
        String v = resolve(key);
        return v.isEmpty() ? fallback : v;
    }

    public static boolean isPresent(String key) {
        return !resolve(key).isEmpty();
    }

    private static Map<String, String> getEnvFileMap() {
        if (envFileMap == null) {
            synchronized (LiveTestEnv.class) {
                if (envFileMap == null) {
                    envFileMap = loadEnvFile();
                }
            }
        }
        return envFileMap;
    }

    private static Map<String, String> loadEnvFile() {
        Map<String, String> map = new LinkedHashMap<>();
        try (var is = LiveTestEnv.class.getClassLoader().getResourceAsStream("env.txt")) {
            if (is == null) {
                return Collections.emptyMap();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String k = line.substring(0, eq).trim();
                        String v = line.substring(eq + 1).trim();
                        map.put(k, v);
                    }
                }
            }
        } catch (IOException ignored) {
            // env.txt 不可读时静默忽略，回退到空 map
        }
        return Collections.unmodifiableMap(map);
    }
}
