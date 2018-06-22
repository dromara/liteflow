package com.thebeastshop.liteflow.parser;

public abstract class ClassXmlFlowParser extends XmlFlowParser {
	@Override
	public void parseMain(String path) throws Exception {
		String content = parseCustom();
		parse(content);
	}
	
	public abstract String parseCustom();
}
