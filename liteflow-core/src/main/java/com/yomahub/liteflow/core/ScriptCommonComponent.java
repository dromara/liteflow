package com.yomahub.liteflow.core;

import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 脚本组件类
 *
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public class ScriptCommonComponent extends NodeComponent implements ScriptComponent {

	private final LFLog LOG = LFLoggerManager.getLogger(this.getClass());

	@Override
	public void process() throws Exception {
		ScriptExecuteWrap wrap = this.buildWrap(this);
		ScriptExecutorFactory.loadInstance().getScriptExecutor(this.getRefNode().getLanguage()).execute(wrap);
	}

	@Override
	public void loadScript(String script, String language) {
		LOG.info("load script for component[{}]", getDisplayName());
		ScriptExecutorFactory.loadInstance().getScriptExecutor(language).load(getNodeId(), script);
	}

}
