package com.yomahub.liteflow.parser;

import cn.hutool.core.io.FileUtil;

/**
 * @Author: guodongqing
 * @Date: 2021/3/26 12:26 下午
 */
public class LocalJsonFlowParser extends JsonFlowParser{

    @Override
    public void parseMain(String rulePath) throws Exception {
        String ruleContent = FileUtil.readUtf8String(rulePath);
        parse(ruleContent);
    }
}
