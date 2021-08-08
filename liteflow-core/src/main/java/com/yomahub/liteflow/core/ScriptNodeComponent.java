package com.yomahub.liteflow.core;

import com.yomahub.liteflow.script.ScriptFactory;

/**
 * 脚本组件类
 * @author Bryan.Zhang
 * @since 2.5.11
 */
public class ScriptNodeComponent extends NodeComponent{

    @Override
    public void process() throws Exception {
        ScriptFactory.loadInstance().getScriptExecutor().execute(getNodeId(), getSlotIndex());
    }

    public void loadScript(String script) {
        ScriptFactory.loadInstance().getScriptExecutor().load(getNodeId(), script);
    }
}
