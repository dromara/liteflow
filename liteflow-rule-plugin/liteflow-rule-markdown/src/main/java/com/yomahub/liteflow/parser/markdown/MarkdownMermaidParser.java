package com.yomahub.liteflow.parser.markdown;

import cn.hutool.core.collection.ListUtil;
import com.google.common.collect.ImmutableList;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.parser.base.FlowParser;
import com.yomahub.liteflow.parser.el.ClassJsonFlowELParser;
import com.yomahub.liteflow.parser.markdown.parser.ChainIdParser;
import com.yomahub.liteflow.parser.markdown.parser.FinalizeParser;
import com.yomahub.liteflow.parser.markdown.parser.FlowNodesParser;
import com.yomahub.liteflow.parser.markdown.parser.GlobalCheckParser;
import com.yomahub.liteflow.parser.markdown.parser.NormalizeParser;
import com.yomahub.liteflow.parser.markdown.parser.Parser;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.spi.holder.PathContentParserHolder;

import java.util.Collections;
import java.util.List;

public class MarkdownMermaidParser extends ClassJsonFlowELParser {



    @Override
    public void parseMain(List<String> pathList) throws Exception {

        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

        parse(PathContentParserHolder.loadContextAware().parseContent(
                ListUtil.toList(liteflowConfig.getRuleSourceExtData().split(",|;")))
        );
    }

    @Override
    public String parseCustom() {
        return null;
    }

    /**
     * #
     * ```mermaid
     * ---
     * title: your chainId here
     * ---
     * flowchart LR
     * A[step description] --> B
     * B --> C{SWITCH specified mark}
     * C -->|One (Comment here will be passed as tag)| D[Result 1]
     * C --Two (Comment here will NOT be passed as tag, just treated as normal comment)--> E[Result 2]
     * ```
     * # Any flowchart below will not be parsed. Only for documentation purpose
     * ```mermaid
     * ---
     * title: Step B design graph
     * ---
     * flowchart LR
     * BStart[step description] --> BStep1[Step 1 description] -->  BFinish[Finish step description]
     * ```
     *
     * @param contentList
     * @throws Exception
     */

    private final List<Parser> parsers = ImmutableList.of(new NormalizeParser(), new GlobalCheckParser(), new ChainIdParser(), new FlowNodesParser(), new FinalizeParser());

    @Override
    public void parse(List<String> contentList) throws Exception {

        contentList.forEach( chainContent -> {

            Parser.ParseContext context = new Parser.ParseContext(chainContent);
            parsers.forEach(p -> p.parse(context));

            LiteFlowChainELBuilder.createChain().setChainId(context.chainId).setEL(context.el).build();

        });

    }
}
