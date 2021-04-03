package com.yomahub.liteflow.parser;

/**
 * 基于自定义的Yml方式解析器
 * @Author: guodongqing
 * @Date: 2021/3/29 4:20 下午
 */
public abstract class ClassYmlFlowParser extends YmlFlowParser{
    @Override
    public void parseMain(String path) throws Exception {
        String content = parseCustom();
        parse(content);
    }

    public abstract String parseCustom();
}
