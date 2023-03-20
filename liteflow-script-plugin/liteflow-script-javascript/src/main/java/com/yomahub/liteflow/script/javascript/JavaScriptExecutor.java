package com.yomahub.liteflow.script.javascript;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.jsr223.JSR223ScriptExecutor;

/**
 * JavaScript脚本语言的执行器实现
 *
 * @author Bryan.Zhang
 * @since 2.9.1
 */
public class JavaScriptExecutor extends JSR223ScriptExecutor {

	@Override
	protected String convertScript(String script) {
		return StrUtil.format("function process(){{}} process();", script);
	}

	@Override
	public ScriptTypeEnum scriptType() {
		return ScriptTypeEnum.JS;
	}

}
