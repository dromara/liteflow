package com.yomahub.liteflow.parser;

import com.alibaba.fastjson.JSONObject;
import com.yomahub.liteflow.parser.base.BaseZookeeperYmlFlowParser;
import com.yomahub.liteflow.parser.helper.ParserHelper;

/**
 * 基于zk方式的yml形式的解析器
 *
 * @author guodongqing
 * @since 2.5.0
 */
public class ZookeeperYmlFlowParser extends BaseZookeeperYmlFlowParser {
	public ZookeeperYmlFlowParser(String node) {
		super(node);
	}

	@Override
	public void parseOneChain(JSONObject chain) {
		ParserHelper.parseOneChain(chain);
	}
}
