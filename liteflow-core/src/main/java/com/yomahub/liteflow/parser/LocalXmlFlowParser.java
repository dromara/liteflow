/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.parser;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import org.springframework.core.io.Resource;

/**
 * 基于本地的xml方式解析器
 * @author Bryan.Zhang
 */
public class LocalXmlFlowParser extends XmlFlowParser{

	public void parseMain(String rulePath) throws Exception {
		Resource[] resources = matchRuleResources(rulePath);
		for (Resource resource : resources) {
			String content = IoUtil.read(resource.getInputStream(), CharsetUtil.CHARSET_UTF_8);
			parse(content);
		}
	}
}
