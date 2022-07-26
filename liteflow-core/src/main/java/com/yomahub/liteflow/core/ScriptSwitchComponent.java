package com.yomahub.liteflow.core;

import com.yomahub.liteflow.script.ScriptExecutorFactory;

/**
 * 脚本条件节点
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public class ScriptSwitchComponent extends NodeSwitchComponent {

    @Override
    public String processSwitch() throws Exception {
        return (String)ScriptExecutorFactory.loadInstance().getScriptExecutor().execute(this.getCurrChainName(), getNodeId(), getSlotIndex());
    }

    public void loadScript(String script) {
        ScriptExecutorFactory.loadInstance().getScriptExecutor().load(getNodeId(), script);
    }
}
