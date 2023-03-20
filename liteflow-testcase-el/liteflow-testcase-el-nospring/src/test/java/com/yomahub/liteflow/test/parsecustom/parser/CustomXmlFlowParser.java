package com.yomahub.liteflow.test.parsecustom.parser;

import com.yomahub.liteflow.parser.el.ClassXmlFlowELParser;

/**
 * 非spring环境的自定义xml parser单元测试 主要测试自定义配置源类是否能引入非spring中的其他依赖
 *
 * @author bryan.zhang
 * @since 2.5.7
 */
public class CustomXmlFlowParser extends ClassXmlFlowELParser {

	@Override
	public String parseCustom() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><nodes><node id=\"a\" class=\"com.yomahub.liteflow.test.parser.cmp.ACmp\"/><node id=\"b\" class=\"com.yomahub.liteflow.test.parser.cmp.BCmp\"/><node id=\"c\" class=\"com.yomahub.liteflow.test.parser.cmp.CCmp\"/><node id=\"d\" class=\"com.yomahub.liteflow.test.parser.cmp.DCmp\"/></nodes><chain name=\"chain1\">THEN(a,b,c,d)</chain></flow>";
	}

}
