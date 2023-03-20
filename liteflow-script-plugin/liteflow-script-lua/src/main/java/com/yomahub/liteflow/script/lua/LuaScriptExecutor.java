package com.yomahub.liteflow.script.lua;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.jsr223.JSR223ScriptExecutor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lua脚本语言的执行器实现
 *
 * @author Bryan.Zhang
 * @since 2.9.5
 */
public class LuaScriptExecutor extends JSR223ScriptExecutor {

	@Override
	public ScriptTypeEnum scriptType() {
		return ScriptTypeEnum.LUA;
	}

	@Override
	protected String convertScript(String script) {
		String[] lineArray = script.split("\\n");
		List<String> noBlankLineList = Arrays.stream(lineArray)
			.filter(s -> !StrUtil.isBlank(s))
			.collect(Collectors.toList());

		// 用第一行的缩进的空格数作为整个代码的缩进量
		String blankStr = ReUtil.getGroup0("^[ ]*", noBlankLineList.get(0));

		// 重新构建脚本
		StringBuilder scriptSB = new StringBuilder();
		noBlankLineList.forEach(s -> scriptSB.append(StrUtil.format("{}\n", s.replaceFirst(blankStr, StrUtil.EMPTY))));
		return scriptSB.toString();
		// return StrUtil.format("function
		// process()\n{}\nend\nprocess()\n",scriptSB.toString());
	}

}
