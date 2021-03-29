package com.yomahub.liteflow.parser;

/**
 * 基于自定义的Json方式解析器
 * @author guodongqing
 * @date 2021-03-29 16:40:00
 */
public abstract class ClassJsonFlowParser extends JsonFlowParser {
	@Override
	public void parseMain(String path) throws Exception {
		String content = parseCustom();
		parse(content);
	}

	public abstract String parseCustom();
}
