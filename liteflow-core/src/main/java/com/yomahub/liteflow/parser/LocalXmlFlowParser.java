/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.parser;

import cn.hutool.core.io.FileUtil;

public class LocalXmlFlowParser extends XmlFlowParser{

	public void parseMain(String rulePath) throws Exception {
		String ENCODING_FORMAT = "UTF-8";
		String ruleContent = FileUtil.readString(rulePath, ENCODING_FORMAT);
		parse(ruleContent);
	}
}
