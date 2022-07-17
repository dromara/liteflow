package com.yomahub.liteflow.parser.el;

import com.yomahub.liteflow.parser.base.BaseZookeeperXmlFlowParser;
import com.yomahub.liteflow.parser.helper.ParserHelper;
import org.dom4j.Element;

/**
 * 基于zk方式的xml形式EL表达式解析器
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class ZookeeperXmlFlowELParser extends BaseZookeeperXmlFlowParser {

	public ZookeeperXmlFlowELParser(String node) {
		super(node);
	}

	@Override
	public void parseOneChain(Element chain) {
		ParserHelper.parseOneChainEl(chain);
	}
}
