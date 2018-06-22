/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-7-28
 * @version 1.0
 */
package com.thebeastshop.liteflow.parser;

import com.thebeastshop.liteflow.util.IOUtil;

public class LocalXmlFlowParser extends XmlFlowParser{

	private final String ENCODING_FORMAT = "UTF-8";
	
	public void parseMain(String rulePath) throws Exception {
		String ruleContent = IOUtil.read(rulePath, ENCODING_FORMAT);
		parse(ruleContent);
	}
}
