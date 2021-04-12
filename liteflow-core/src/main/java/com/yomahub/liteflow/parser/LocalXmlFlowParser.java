/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.parser;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 基于本地的xml方式解析器
 * @author Bryan.Zhang
 */
public class LocalXmlFlowParser extends XmlFlowParser{

	public void parseMain(String rulePath) throws Exception {
		String ruleContent = ResourceUtil.readUtf8Str(StrUtil.format("classpath:{}",rulePath));
		parse(ruleContent);
	}
}
