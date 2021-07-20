package com.yomahub.liteflow.parser;

import cn.hutool.core.collection.ListUtil;

import java.util.List;

/**
 * 基于自定义的Yml方式解析器
 * @author guodongqing
 * @since 2.5.0
 */
public abstract class ClassYmlFlowParser extends YmlFlowParser{
    @Override
    public void parseMain(List<String> pathList) throws Exception {
        String content = parseCustom();
        parse(content);
    }

    public abstract String parseCustom();
}
