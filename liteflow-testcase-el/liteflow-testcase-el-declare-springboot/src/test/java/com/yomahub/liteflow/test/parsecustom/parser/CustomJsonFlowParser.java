package com.yomahub.liteflow.test.parsecustom.parser;

import com.yomahub.liteflow.parser.el.ClassJsonFlowELParser;

/**
 * 模拟用户自定义源解析
 * @author dongguo.tao
 * @since 2.5.0
 */
public class CustomJsonFlowParser extends ClassJsonFlowELParser {
    @Override
    public String parseCustom() {
        //模拟自定义解析结果
        String content = "{\"flow\":{\"nodes\":{\"node\":[{\"id\":\"a\",\"class\":\"com.yomahub.liteflow.test.parsecustom.cmp.ACmp\"},{\"id\":\"b\",\"class\":\"com.yomahub.liteflow.test.parsecustom.cmp.BCmp\"},{\"id\":\"c\",\"class\":\"com.yomahub.liteflow.test.parsecustom.cmp.CCmp\"},{\"id\":\"d\",\"class\":\"com.yomahub.liteflow.test.parsecustom.cmp.DCmp\"},{\"id\":\"e\",\"class\":\"com.yomahub.liteflow.test.parsecustom.cmp.ECmp\"},{\"id\":\"f\",\"class\":\"com.yomahub.liteflow.test.parsecustom.cmp.FCmp\"},{\"id\":\"g\",\"class\":\"com.yomahub.liteflow.test.parsecustom.cmp.GCmp\"}]},\"chain\":[{\"name\":\"chain2\",\"value\":\"THEN(c, g, f)\"},{\"name\":\"chain1\",\"value\":\"THEN(a, c, WHEN(b, d, SWITCH(e).to(f, g), chain2))\"}]}}";
        return content;
    }
}
