package com.yomahub.liteflow.parser.el;

import java.util.List;

/**
 * 基于自定义的xml方式EL表达式解析器
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public abstract class ClassXmlFlowELParser extends XmlFlowELParser {

	@Override
	public void parseMain(List<String> pathList) throws Exception {
		String content = parseCustom();
		parse(content);
	}

	public abstract String parseCustom();

}
