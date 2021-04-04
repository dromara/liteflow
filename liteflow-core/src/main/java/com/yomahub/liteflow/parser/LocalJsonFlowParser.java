package com.yomahub.liteflow.parser;

import cn.hutool.core.io.FileUtil;

/**
 * @author: guodongqing
 * @since: 2.5.0
 */
public class LocalJsonFlowParser extends JsonFlowParser{

    @Override
    public void parseMain(String rulePath) throws Exception {
        String ruleContent = FileUtil.readUtf8String(rulePath);
        parse(ruleContent);
    }
}
