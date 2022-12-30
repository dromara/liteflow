package com.yomahub.liteflow.parser.markdown.parser;

import java.util.StringJoiner;

public class FinalizeParser implements Parser{
    @Override
    public void parse(ParseContext context) {
        StringJoiner main = new StringJoiner(",", "THEN(", ")");
        join(context.head, main);
        context.el = main.toString();
    }

    private void join(FlowChartNode root, StringJoiner main) {
        switch (root.type) {
            case COMMON:
                main.add("node(\"" + root.nid + "\")");

                if (root.next.isEmpty()) {
                    return;
                }

                if (root.next.size() == 1) {
                    join(root.next.get(0), main);
                } else {
                    StringJoiner when = new StringJoiner(",", "WHEN(", ")");
                    root.next.forEach(n -> {
                        StringJoiner subMain = new StringJoiner(",", "THEN(", ")");
                        join(n, subMain);
                        when.add(subMain.toString());
                    });
                    main.add(when.toString());
                }

                break;
            case SWITCH:
                StringJoiner sw = new StringJoiner(",", "SWITCH(node(\"" + root.nid + "\")).to(", ")");
                root.next.forEach(n -> {
                    StringJoiner subMain = new StringJoiner(",", "THEN(", ").id(\"" + n.nid + "\")");
                    join(n, subMain);
                    sw.add(subMain.toString());
                });
                main.add(sw.toString());
                break;
            default:
                throw new IllegalStateException("FIXME unknown type:" + root.type);
        }


    }
}
