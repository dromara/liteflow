/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.parser;

import com.yomahub.liteflow.spi.holder.PathContentParserHolder;

import java.util.List;

/**
 * 基于本地的xml方式解析器
 * @author Bryan.Zhang
 */
public class LocalXmlFlowParser extends XmlFlowParser{

	@Override
	public void parseMain(List<String> pathList) throws Exception {
		List<String> contentList = PathContentParserHolder.loadContextAware().parseContent(pathList);
		parse(contentList);
	}
}
