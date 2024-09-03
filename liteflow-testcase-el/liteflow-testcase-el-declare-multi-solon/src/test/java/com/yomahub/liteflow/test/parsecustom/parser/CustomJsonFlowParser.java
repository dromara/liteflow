package com.yomahub.liteflow.test.parsecustom.parser;

import com.yomahub.liteflow.parser.el.ClassJsonFlowELParser;

/**
 * 模拟用户自定义源解析
 *
 * @author dongguo.tao
 * @since 2.5.0
 */
public class CustomJsonFlowParser extends ClassJsonFlowELParser {

	@Override
	public String parseCustom() {
		// 模拟自定义解析结果
		String content = "{\n" + "    \"flow\": {\n" + "        \"chain\": [\n" + "            {\n"
				+ "                \"name\": \"chain2\",\n" + "                \"value\": \"THEN(c, g, f)\"\n"
				+ "            },\n" + "            {\n" + "                \"name\": \"chain1\",\n"
				+ "                \"value\": \"THEN(a, c, WHEN(b, d, SWITCH(e).to(f, g), chain2))\"\n"
				+ "            }\n" + "        ]\n" + "    }\n" + "}";
		System.out.println(content);
		return content;
	}

}
