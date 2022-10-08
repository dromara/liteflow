package com.yomahub.liteflow.core;

import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutorFactory;

/**
 * 脚本IF节点
 * @author Bryan.Zhang
 * @since 2.8.5
 */
public class ScriptIfComponent extends NodeIfComponent implements ScriptComponent{
    @Override
    public boolean processIf() throws Exception {
        ScriptExecuteWrap wrap = new ScriptExecuteWrap();
        wrap.setCurrChainName(this.getCurrChainName());
        wrap.setNodeId(this.getNodeId());
        wrap.setSlotIndex(this.getSlotIndex());
        wrap.setCmpData(this.getCmpData(Object.class));
        return (boolean)ScriptExecutorFactory.loadInstance().getScriptExecutor().execute(wrap);
    }

    @Override
    public void loadScript(String script) {
        ScriptExecutorFactory.loadInstance().getScriptExecutor().load(getNodeId(), script);
    }
}
