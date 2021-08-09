package com.yomahub.liteflow.core;

import com.yomahub.liteflow.script.ScriptExecutorFactory;

/**
 * 脚本条件节点
 * @author Bryan.Zhang
 * @since 2.5.11
 */
public class ScriptCondComponent extends NodeCondComponent{

    @Override
    public String processCond() throws Exception {
        return (String)ScriptExecutorFactory.loadInstance().getScriptExecutor().execute(getNodeId(), getSlotIndex());
    }

    public void loadScript(String script) {
        ScriptExecutorFactory.loadInstance().getScriptExecutor().load(getNodeId(), script);
    }
}
