package com.yomahub.liteflow.script.groovy;

import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.jsr223.JSR223ScriptExecutor;

/**
 * Groovy脚本语言的执行器实现
 *
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public class GroovyScriptExecutor extends JSR223ScriptExecutor {

	@Override
	public ScriptTypeEnum scriptType() {
		return ScriptTypeEnum.GROOVY;
	}

}
