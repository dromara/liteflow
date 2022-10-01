package com.yomahub.liteflow.core;

import com.yomahub.liteflow.script.ScriptExecutorFactory;

/**
 * 脚本BREAK节点
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public class ScriptBreakComponent extends NodeBreakComponent implements ScriptComponent{
    @Override
    public boolean processBreak() throws Exception {
        return (boolean) ScriptExecutorFactory.loadInstance().getScriptExecutor().execute(this.getCurrChainName(), getNodeId(), getSlotIndex());
    }

    @Override
    public void loadScript(String script) {
        ScriptExecutorFactory.loadInstance().getScriptExecutor().load(getNodeId(), script);
    }
}
