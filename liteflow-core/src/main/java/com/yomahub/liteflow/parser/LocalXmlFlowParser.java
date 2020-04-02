/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.parser;

import com.yomahub.liteflow.util.IOUtil;

public class LocalXmlFlowParser extends XmlFlowParser{

	private final String ENCODING_FORMAT = "UTF-8";

	public void parseMain(String rulePath) throws Exception {
		String ruleContent = IOUtil.read(rulePath, ENCODING_FORMAT);
		parse(ruleContent);
	}
}
