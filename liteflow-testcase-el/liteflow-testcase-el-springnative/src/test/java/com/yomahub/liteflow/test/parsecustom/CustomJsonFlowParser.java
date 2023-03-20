package com.yomahub.liteflow.test.parsecustom;

import com.yomahub.liteflow.parser.el.ClassJsonFlowELParser;

/**
 * 模拟用户自定义源解析
 *
 * @author dongguo.tao
 * @date 2021/4/7
 */
public class CustomJsonFlowParser extends ClassJsonFlowELParser {

	@Override
	public String parseCustom() {
		// 模拟自定义解析结果
		String content = "{\"flow\":{\"chain\":[{\"name\":\"chain2\",\"value\":\"THEN(c,g,f)\"},{\"name\":\"chain1\",\"value\":\"THEN(a,c,WHEN(b,d,SWITCH(e).to(f,g)), chain2)\"}]}}";
		return content;
	}

}
