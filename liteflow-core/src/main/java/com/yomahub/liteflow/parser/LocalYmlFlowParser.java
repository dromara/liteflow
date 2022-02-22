package com.yomahub.liteflow.parser;

import com.yomahub.liteflow.spi.factory.PathContentParserFactory;

import java.util.List;

/**
 * Yaml格式转换
 *
 * @author guodongqing
 * @since 2.5.0
 */
public class LocalYmlFlowParser extends YmlFlowParser {

    @Override
    public void parseMain(List<String> pathList) throws Exception {
        List<String> contentList = PathContentParserFactory.loadContextAware().parseContent(pathList);
        parse(contentList);
    }

}
