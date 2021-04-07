package com.yomahub.liteflow.test.parsecustom;

import com.yomahub.liteflow.parser.ClassJsonFlowParser;

/**
 * 模拟用户自定义源解析
 * @author dongguo.tao
 * @date 2021/4/7
 */
public class CustomJsonFlowParser extends ClassJsonFlowParser {
    @Override
    public String parseCustom() {
        //模拟自定义解析结果
        String content = "{\"flow\":{\"nodes\":{\"node\":[{\"id\":\"a\",\"class\":\"com.yomahub.liteflow.test.parsecustom.cmp.ACmp\"},{\"id\":\"b\",\"class\":\"com.yomahub.liteflow.test.parsecustom.cmp.BCmp\"},{\"id\":\"c\",\"class\":\"com.yomahub.liteflow.test.parsecustom.cmp.CCmp\"},{\"id\":\"d\",\"class\":\"com.yomahub.liteflow.test.parsecustom.cmp.DCmp\"},{\"id\":\"e\",\"class\":\"com.yomahub.liteflow.test.parsecustom.cmp.ECmp\"},{\"id\":\"f\",\"class\":\"com.yomahub.liteflow.test.parsecustom.cmp.FCmp\"},{\"id\":\"g\",\"class\":\"com.yomahub.liteflow.test.parsecustom.cmp.GCmp\"}]},\"chain\":[{\"name\":\"chain2\",\"condition\":[{\"type\":\"then\",\"value\":\"c,g,f\"}]},{\"name\":\"chain1\",\"condition\":[{\"type\":\"then\",\"value\":\"a,c\"},{\"type\":\"when\",\"value\":\"b,d,e(f|g)\"},{\"type\":\"then\",\"value\":\"chain2\"}]}]}}";
        return content;
    }
}
