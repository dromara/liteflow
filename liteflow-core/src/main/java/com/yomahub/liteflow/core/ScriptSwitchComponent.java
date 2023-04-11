package com.yomahub.liteflow.core;

import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutorFactory;

import java.util.Map;

/**
 * 脚本条件节点
 *
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public class ScriptSwitchComponent extends NodeSwitchComponent implements ScriptComponent {

	@Override
	public String processSwitch() throws Exception {
		ScriptExecuteWrap wrap = this.buildWrap(this);
		return (String) ScriptExecutorFactory.loadInstance()
			.getScriptExecutor(this.getRefNode().getLanguage())
			.execute(wrap);
	}

	@Override
	public void loadScript(String script, String language) {
		ScriptExecutorFactory.loadInstance().getScriptExecutor(language).load(getNodeId(), script);
	}

}
