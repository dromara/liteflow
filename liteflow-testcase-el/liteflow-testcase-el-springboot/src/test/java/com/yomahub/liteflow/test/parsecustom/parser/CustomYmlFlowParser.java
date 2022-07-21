package com.yomahub.liteflow.test.parsecustom.parser;

import com.yomahub.liteflow.parser.el.ClassYmlFlowELParser;

/**
 * 模拟用户自定义源解析
 *
 * @author junjun
 */
public class CustomYmlFlowParser extends ClassYmlFlowELParser {
    @Override
    public String parseCustom() {
        //模拟自定义解析结果
        return "flow:\n" +
                "  chain:\n" +
                "    - name: chain1\n" +
                "      value: \"THEN(a, b, c);\"";
    }
}
