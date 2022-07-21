package com.yomahub.liteflow.test.parsecustom.parser;

import com.yomahub.liteflow.parser.ClassYmlFlowParser;

/**
 * springboot环境的自定义yml parser单元测试
 * 主要测试自定义配置源类是否能引入springboot中的其他依赖
 * <p>
 *
 * @author junjun
 */
public class CustomYmlFlowParser extends ClassYmlFlowParser {

    @Override
    public String parseCustom() {
        return "flow:\n" +
                "  chain:\n" +
                "    - name: chain1\n" +
                "      condition:\n" +
                "        - type: then\n" +
                "          value: 'a,b,c'";
    }
}