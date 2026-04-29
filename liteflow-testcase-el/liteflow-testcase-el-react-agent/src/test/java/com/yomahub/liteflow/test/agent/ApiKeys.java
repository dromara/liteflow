package com.yomahub.liteflow.test.agent;

/**
 * 示例测试 API Key 工具：环境变量优先，再回退到 Spring 注入的 properties。
 * 留空时调用 {@link org.junit.jupiter.api.Assumptions#assumeTrue} 跳过用例。
 */
public final class ApiKeys {

    private ApiKeys() {}

    public static String resolve(String envName, String configured) {
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) return env.trim();
        return configured == null ? "" : configured.trim();
    }

    public static boolean isPresent(String s) {
        return s != null && !s.isBlank();
    }
}
