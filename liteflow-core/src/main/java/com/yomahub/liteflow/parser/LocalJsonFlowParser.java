package com.yomahub.liteflow.parser;

import com.yomahub.liteflow.spi.holder.PathContentParserHolder;

import java.util.List;

/**
 * @author guodongqing
 * @since 2.5.0
 */
public class LocalJsonFlowParser extends JsonFlowParser{

    @Override
    public void parseMain(List<String> pathList) throws Exception {
        List<String> contentList = PathContentParserHolder.loadContextAware().parseContent(pathList);
        parse(contentList);
    }
}
