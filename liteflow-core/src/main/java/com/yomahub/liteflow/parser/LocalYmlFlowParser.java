package com.yomahub.liteflow.parser;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.CharsetUtil;
import org.springframework.core.io.Resource;

/**
 * Yaml格式转换
 *
 * @author guodongqing
 * @since 2.5.0
 */
public class LocalYmlFlowParser extends YmlFlowParser {

    @Override
    public void parseMain(String rulePath) throws Exception {
        Resource[] resources = matchRuleResources(rulePath);
        for (Resource resource : resources) {
            String content = IoUtil.read(resource.getInputStream(), CharsetUtil.CHARSET_UTF_8);
            parse(content);
        }
    }

}
