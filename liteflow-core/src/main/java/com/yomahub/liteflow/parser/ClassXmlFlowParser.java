package com.yomahub.liteflow.parser;

/**
 * 基于自定义的xml方式解析器
 * @author Bryan.Zhang
 */
public abstract class ClassXmlFlowParser extends XmlFlowParser {
	@Override
	public void parseMain(String path) throws Exception {
		String content = parseCustom();
		parse(content);
	}

	public abstract String parseCustom();
}
