package com.yomahub.liteflow.parser.markdown.parser;

public class GlobalCheckParser implements Parser{
    @Override
    public void parse(ParseContext context) {
        String[] lines = context.lines;
        if (lines.length < 5) {
            throw new IllegalArgumentException("empty flowchart");
        }

        if (!lines[0].startsWith("---")
                || !lines[1].startsWith(ChainIdParser.TITLE_PREFIX)
                || !lines[2].startsWith("---") ) {
            throw new IllegalArgumentException("should has title for specifying chainId");
        }

        if (!lines[3].startsWith("flowchart")) {
            throw new IllegalArgumentException("should be a flowchart");
        }
    }
}
