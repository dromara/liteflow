package com.yomahub.liteflow.core;

import com.yomahub.liteflow.script.ScriptExecutorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 脚本组件类
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public class ScriptComponent extends NodeComponent{

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void process() throws Exception {
        ScriptExecutorFactory.loadInstance().getScriptExecutor().execute(getNodeId(), getSlotIndex());
    }

    public void loadScript(String script) {
        log.info("load script for component[{}][{}]", getNodeId(), getDisplayName());
        ScriptExecutorFactory.loadInstance().getScriptExecutor().load(getNodeId(), script);
    }
}
