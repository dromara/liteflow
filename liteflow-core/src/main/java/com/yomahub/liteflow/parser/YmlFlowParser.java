package com.yomahub.liteflow.parser;

import com.alibaba.fastjson.JSONObject;
import com.yomahub.liteflow.parser.base.BaseYmlFlowParser;
import com.yomahub.liteflow.parser.helper.ParserHelper;

/**
 * Yml格式解析器，转换为json格式进行解析
 * @author guodongqing
 * @since 2.5.0
 */
public abstract class YmlFlowParser extends BaseYmlFlowParser {

    /**
     * 解析一个chain的过程
     */
    public void parseOneChain(JSONObject chainObject) {
        ParserHelper.parseOneChain(chainObject);
    }

}
