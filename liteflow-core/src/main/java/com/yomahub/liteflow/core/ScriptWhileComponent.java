package com.yomahub.liteflow.core;

import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutorFactory;

import java.util.Map;

/**
 * 脚本WHILE节点
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public class ScriptWhileComponent extends NodeWhileComponent implements ScriptComponent {

	@Override
	public boolean processWhile() throws Exception {
		ScriptExecuteWrap wrap = this.buildWrap(this);
		return (boolean) ScriptExecutorFactory.loadInstance()
			.getScriptExecutor(this.getRefNode().getLanguage())
			.execute(wrap);
	}

	@Override
	public void loadScript(String script, String language) {
		ScriptExecutorFactory.loadInstance().getScriptExecutor(language).load(getNodeId(), script);
	}

}
