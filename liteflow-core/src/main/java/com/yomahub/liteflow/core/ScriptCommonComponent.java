package com.yomahub.liteflow.core;

import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 脚本组件类
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public class ScriptCommonComponent extends NodeComponent implements ScriptComponent{

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void process() throws Exception {
        ScriptExecuteWrap wrap = new ScriptExecuteWrap();
        wrap.setCurrChainId(this.getCurrChainId());
        wrap.setNodeId(this.getNodeId());
        wrap.setSlotIndex(this.getSlotIndex());
        wrap.setTag(this.getTag());
        wrap.setCmpData(this.getCmpData(Map.class));
        ScriptExecutorFactory.loadInstance().getScriptExecutor().execute(wrap);
    }

    @Override
    public void loadScript(String script) {
        log.info("load script for component[{}]", getDisplayName());
        ScriptExecutorFactory.loadInstance().getScriptExecutor().load(getNodeId(), script);
    }
}
