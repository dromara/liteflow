package com.yomahub.liteflow.parser;

import cn.hutool.core.collection.ListUtil;

import java.util.List;

/**
 * 基于自定义的xml方式解析器
 * @author Bryan.Zhang
 */
public abstract class ClassXmlFlowParser extends XmlFlowParser {
	@Override
	public void parseMain(List<String> pathList) throws Exception {
		String content = parseCustom();
		parse(content);
	}

	public abstract String parseCustom();
}
