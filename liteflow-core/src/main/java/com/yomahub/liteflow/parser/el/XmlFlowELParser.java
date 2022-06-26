package com.yomahub.liteflow.parser.el;

import com.yomahub.liteflow.parser.base.BaseXmlFlowParser;
import com.yomahub.liteflow.parser.helper.ParserHelper;
import org.dom4j.Element;

/**
 * Xml形式的EL表达式解析抽象引擎
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public abstract class XmlFlowELParser extends BaseXmlFlowParser {

	/**
	 * 解析一个chain的过程
	 */
	@Override
	public void parseOneChain(Element e) {
		ParserHelper.parseOneChainEl(e);
	}

}
