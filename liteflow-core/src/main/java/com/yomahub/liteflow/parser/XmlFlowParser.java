package com.yomahub.liteflow.parser;

import com.yomahub.liteflow.parser.base.BaseXmlFlowParser;
import com.yomahub.liteflow.parser.helper.ParserHelper;
import org.dom4j.Element;

;

/**
 * xml形式的解析器
 *
 * @author Bryan.Zhang
 */
public abstract class XmlFlowParser extends BaseXmlFlowParser {

	/**
	 * 解析一个chain的过程
	 */
	public void parseOneChain(Element e) {
		ParserHelper.parseOneChain(e);
	}

}
