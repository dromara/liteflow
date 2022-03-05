package com.yomahub.liteflow.test.parsecustom.parser;

import com.yomahub.liteflow.parser.ClassXmlFlowParser;

/**
 * 非spring环境的自定义xml parser单元测试
 * 主要测试自定义配置源类是否能引入springboot中的其他依赖
 * @author bryan.zhang
 * @since 2.5.7
 */
public class CustomXmlFlowParser extends ClassXmlFlowParser {

    @Override
    public String parseCustom() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><nodes><node id=\"a\" class=\"com.yomahub.liteflow.test.parsecustom.cmp.ACmp\"/><node id=\"b\" class=\"com.yomahub.liteflow.test.parsecustom.cmp.BCmp\"/><node id=\"c\" class=\"com.yomahub.liteflow.test.parsecustom.cmp.CCmp\"/><node id=\"d\" class=\"com.yomahub.liteflow.test.parsecustom.cmp.DCmp\"/><node id=\"e\" class=\"com.yomahub.liteflow.test.parsecustom.cmp.ECmp\"/><node id=\"f\" class=\"com.yomahub.liteflow.test.parsecustom.cmp.FCmp\"/><node id=\"g\" class=\"com.yomahub.liteflow.test.parsecustom.cmp.GCmp\"/></nodes><chain name=\"chain1\"><then value=\"a,b,c,d\"/></chain></flow>";
    }
}
