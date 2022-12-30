package com.yomahub.liteflow.parser.markdown.parser;

import java.util.Arrays;

public class NormalizeParser implements Parser{
    @Override
    public void parse(ParseContext context) {
        context.lines = context.rawText
                .replaceAll("\r", "")
                .replaceAll("\\n\\s+", "\n")
                .replaceAll("\\s+\\n", "\n")
                .replaceAll(";\\s*\\n", "\n")
                .replaceAll("\\n+", "\n")
                .split("\n");

        int start = 0, end = 0;
        for (int i = 0; i < context.lines.length; i++) {
            if (context.lines[i].equals("```mermaid")) {
                start = i;
            } else if (context.lines[i].equals("```")) {
                end = i;
                break;
            }
        }

        context.lines = Arrays.copyOfRange(context.lines, start + 1, end);
    }
}
