package com.yomahub.liteflow.property.agent;

/**
 * 1.x local-file memory 配置的绑定兼容对象，仅用于旧配置迁移诊断。
 *
 * <p>AgentScope 2 runtime 不读取此对象。文件持久化请改用
 * {@code liteflow.agent.state-store.type=JSON} 与
 * {@code liteflow.agent.state-store.json-root}；自定义存储请注册
 * {@code AgentStateStore} Bean，并选择 {@code state-store.type=BEAN}。
 */
public class LocalFileMemoryConfig {

    /** 历史目录名常量，仅保留给源码兼容；AgentScope 2 state store 不读取。 */
    public static final String SUB_DIR = ".agent-session";
}
