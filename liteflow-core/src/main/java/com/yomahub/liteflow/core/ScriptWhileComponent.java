package com.yomahub.liteflow.core;

import com.yomahub.liteflow.script.ScriptExecutorFactory;

/**
 * 脚本WHILE节点
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public class ScriptWhileComponent extends NodeWhileComponent{

    @Override
    public boolean processWhile() throws Exception {
        return (boolean) ScriptExecutorFactory.loadInstance().getScriptExecutor().execute(this.getCurrChainName(), getNodeId(), getSlotIndex());
    }

    public void loadScript(String script) {
        ScriptExecutorFactory.loadInstance().getScriptExecutor().load(getNodeId(), script);
    }
}
