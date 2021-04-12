package com.yomahub.liteflow.parser;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;

/**
 * @author guodongqing
 * @since 2.5.0
 */
public class LocalJsonFlowParser extends JsonFlowParser{

    @Override
    public void parseMain(String rulePath) throws Exception {
        String ruleContent = ResourceUtil.readUtf8Str(StrUtil.format("classpath:{}",rulePath));
        parse(ruleContent);
    }
}
