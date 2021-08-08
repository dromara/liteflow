package com.yomahub.liteflow.script;

/**
 * 脚本执行器
 * @author Bryan.Zhang
 * @since 2.5.11
 */
public interface ScriptExecutor {

    ScriptExecutor init();

    void load(String nodeId, String script);

    void execute(String nodeId, int slotIndex);
}
