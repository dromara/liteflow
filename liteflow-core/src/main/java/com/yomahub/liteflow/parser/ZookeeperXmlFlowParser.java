package com.yomahub.liteflow.parser;

import com.yomahub.liteflow.parser.base.BaseZookeeperXmlFlowParser;
import com.yomahub.liteflow.parser.helper.ParserHelper;
import org.dom4j.Element;

/**
 * 基于zk方式的xml形式的解析器
 * @author Bryan.Zhang
 */
public class ZookeeperXmlFlowParser extends BaseZookeeperXmlFlowParser {

	public ZookeeperXmlFlowParser(String node) {
		super(node);
	}

	@Override
	public void parseOneChain(Element chain) {
		ParserHelper.parseOneChain(chain);
	}
}
