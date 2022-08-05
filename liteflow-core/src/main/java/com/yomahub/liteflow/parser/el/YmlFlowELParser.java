package com.yomahub.liteflow.parser.el;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.parser.base.BaseYmlFlowParser;
import com.yomahub.liteflow.parser.helper.ParserHelper;

/**
 * yml形式的EL表达式解析抽象引擎
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public abstract class YmlFlowELParser extends BaseYmlFlowParser {

	/**
	 * 解析一个chain的过程
	 */
	@Override
	public void parseOneChain(JsonNode chainObject) {
		ParserHelper.parseOneChainEl(chainObject);
	}

}
