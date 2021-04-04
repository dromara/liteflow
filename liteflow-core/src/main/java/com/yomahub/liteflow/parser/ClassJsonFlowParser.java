package com.yomahub.liteflow.parser;

/**
 * 基于自定义的Json方式解析器
 * @author guodongqing
 * @since 1.2.5
 */
public abstract class ClassJsonFlowParser extends JsonFlowParser {
	@Override
	public void parseMain(String path) throws Exception {
		String content = parseCustom();
		parse(content);
	}

	public abstract String parseCustom();
}
