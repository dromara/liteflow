package com.yomahub.liteflow.parser.el;

import com.yomahub.liteflow.spi.holder.PathContentParserHolder;

import java.util.List;

/**
 * 基于本地的json方式EL表达式解析器
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class LocalJsonFlowELParser extends JsonFlowELParser {

	@Override
	public void parseMain(List<String> pathList) throws Exception {
		List<String> contentList = PathContentParserHolder.loadContextAware().parseContent(pathList);
		parse(contentList);
	}

}
