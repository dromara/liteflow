package com.yomahub.liteflow.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.parser.base.BaseJsonFlowParser;
import com.yomahub.liteflow.parser.helper.ParserHelper;

/**
 * Json格式解析器
 *
 * @author guodongqing
 * @since 2.5.0
 */
public abstract class JsonFlowParser extends BaseJsonFlowParser {

	/**
	 * 解析一个chain的过程
	 */
	@Override
	public void parseOneChain(JsonNode chainObject) {
		ParserHelper.parseOneChain(chainObject);
	}

}
