package com.yomahub.liteflow.parser.factory;

import com.yomahub.liteflow.parser.*;
import com.yomahub.liteflow.parser.el.*;

/**
 * 本地文件
 * <p>
 *
 * @author junjun
 */
public class LocalParserFactory implements FlowParserFactory {

	@Override
	public JsonFlowELParser createJsonELParser(String path) {
		return new LocalJsonFlowELParser();
	}

	@Override
	public XmlFlowELParser createXmlELParser(String path) {
		return new LocalXmlFlowELParser();
	}

	@Override
	public YmlFlowELParser createYmlELParser(String path) {
		return new LocalYmlFlowELParser();
	}

}
