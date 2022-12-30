package com.yomahub.liteflow.parser.spi.markdown;

import com.yomahub.liteflow.parser.markdown.MarkdownMermaidParser;
import com.yomahub.liteflow.parser.spi.ParserClassNameSpi;

public class MarkdownParserClassNameSpi implements ParserClassNameSpi {
    @Override
    public String getSpiClassName() {
        return MarkdownMermaidParser.class.getName();
    }
}
