package com.yomahub.liteflow.parser.el;

import com.alibaba.fastjson.JSONObject;
import com.yomahub.liteflow.parser.base.BaseZookeeperJsonFlowParser;
import com.yomahub.liteflow.parser.helper.ParserHelper;

/**
 * 基于zk方式的json形式的解析器
 * @author guodongqing
 * @since 2.5.0
 */
public class ZookeeperJsonFlowELParser extends BaseZookeeperJsonFlowParser {

    public ZookeeperJsonFlowELParser(String node) {
        super(node);
    }

    @Override
    public void parseOneChain(JSONObject chainObject) {
        ParserHelper.parseOneChainEl(chainObject);
    }

}
