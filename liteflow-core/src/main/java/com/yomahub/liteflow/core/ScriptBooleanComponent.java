package com.yomahub.liteflow.core;

import com.yomahub.liteflow.script.ScriptExecuteWrap;
import com.yomahub.liteflow.script.ScriptExecutorFactory;

/**
 * 脚本BOOLEAN节点
 *
 * @author Bryan.Zhang
 * @since 2.12.0
 */
public class ScriptBooleanComponent extends NodeBooleanComponent implements ScriptComponent {

	@Override
	public boolean processBoolean() throws Exception {
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
