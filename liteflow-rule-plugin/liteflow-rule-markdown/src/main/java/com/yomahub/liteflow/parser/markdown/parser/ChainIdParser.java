package com.yomahub.liteflow.parser.markdown.parser;

public class ChainIdParser implements Parser{

    static final String TITLE_PREFIX = "title: ";

    @Override
    public void parse(ParseContext context) {
        final String chainIdRaw = context.lines[1];

        context.chainId = chainIdRaw.substring(TITLE_PREFIX.length());

    }
}
