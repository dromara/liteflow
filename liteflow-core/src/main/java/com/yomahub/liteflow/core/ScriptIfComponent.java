package com.yomahub.liteflow.core;

import com.yomahub.liteflow.script.ScriptExecutorFactory;

/**
 * 脚本IF节点
 * @author Bryan.Zhang
 * @since 2.8.5
 */
public class ScriptIfComponent extends NodeIfComponent{
    @Override
    public boolean processIf() throws Exception {
        return (boolean)ScriptExecutorFactory.loadInstance().getScriptExecutor().execute(this.getCurrChainName(), getNodeId(), getSlotIndex());
    }

    public void loadScript(String script) {
        ScriptExecutorFactory.loadInstance().getScriptExecutor().load(getNodeId(), script);
    }
}
