package com.yomahub.liteflow.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.parser.base.BaseZookeeperJsonFlowParser;
import com.yomahub.liteflow.parser.helper.ParserHelper;

/**
 * 基于zk方式的json形式的解析器
 * @author guodongqing
 * @since 2.5.0
 */
public class ZookeeperJsonFlowParser extends BaseZookeeperJsonFlowParser {

    public ZookeeperJsonFlowParser(String node) {
        super(node);
    }

    @Override
    public void parseOneChain(JsonNode chainObject) {
        ParserHelper.parseOneChain(chainObject);
    }

}
