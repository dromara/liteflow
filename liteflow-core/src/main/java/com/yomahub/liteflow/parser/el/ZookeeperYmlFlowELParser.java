package com.yomahub.liteflow.parser.el;

import com.alibaba.fastjson.JSONObject;
import com.yomahub.liteflow.parser.base.BaseZookeeperYmlFlowParser;
import com.yomahub.liteflow.parser.helper.ParserHelper;

/**
 * 基于zk方式的yml形式EL表达式解析器
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class ZookeeperYmlFlowELParser extends BaseZookeeperYmlFlowParser {
	public ZookeeperYmlFlowELParser(String node) {
		super(node);
	}

	@Override
	public void parseOneChain(JSONObject chain) {
		ParserHelper.parseOneChainEl(chain);
	}
}
