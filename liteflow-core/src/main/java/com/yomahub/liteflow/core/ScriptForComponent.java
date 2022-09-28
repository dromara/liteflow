package com.yomahub.liteflow.core;

import com.yomahub.liteflow.script.ScriptExecutorFactory;

/**
 * 脚本FOR节点
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public class ScriptForComponent extends NodeForComponent{
    @Override
    public int processFor() throws Exception {
        return (int) ScriptExecutorFactory.loadInstance().getScriptExecutor().execute(this.getCurrChainName(), getNodeId(), getSlotIndex());
    }

    public void loadScript(String script) {
        ScriptExecutorFactory.loadInstance().getScriptExecutor().load(getNodeId(), script);
    }
}
