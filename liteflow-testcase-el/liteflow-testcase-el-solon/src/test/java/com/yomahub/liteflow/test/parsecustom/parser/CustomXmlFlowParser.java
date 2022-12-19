package com.yomahub.liteflow.test.parsecustom.parser;

import com.yomahub.liteflow.parser.el.ClassXmlFlowELParser;
import com.yomahub.liteflow.test.parsecustom.bean.TestBean;
import org.noear.solon.annotation.Inject;

/**
 * springboot环境的自定义xml parser单元测试
 * 主要测试自定义配置源类是否能引入springboot中的其他依赖
 * @author bryan.zhang
 * @since 2.5.7
 */
public class CustomXmlFlowParser extends ClassXmlFlowELParser {

    @Inject
    private TestBean testBean;

    @Override
    public String parseCustom() {
        return testBean.returnXmlContent();
    }
}
