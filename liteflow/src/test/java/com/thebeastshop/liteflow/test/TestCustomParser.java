package com.thebeastshop.liteflow.test;

import com.thebeastshop.liteflow.parser.ClassXmlFlowParser;

public class TestCustomParser extends ClassXmlFlowParser {

	@Override
	public String parseCustom() {
		System.out.println("进入自定义parser,这里只做进入作用，不返回具体xml");
		return null;
	}

}
