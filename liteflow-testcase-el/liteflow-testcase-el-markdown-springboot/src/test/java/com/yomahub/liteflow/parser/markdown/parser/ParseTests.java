package com.yomahub.liteflow.parser.markdown.parser;

import cn.hutool.core.io.resource.ResourceUtil;
import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParseTests {
    private final List<Parser> parsers = ImmutableList.of(new NormalizeParser(), new GlobalCheckParser(), new ChainIdParser(), new FlowNodesParser(), new FinalizeParser());

    @Test
    public void testFlowNodesParser() throws Exception{
        String text = ResourceUtil.readUtf8Str("classpath://CASE1.md");
        Parser.ParseContext parseContext = new Parser.ParseContext(text);

        parsers.forEach(p ->p.parse(parseContext));

        Assert.assertTrue("1>>>" + parseContext.chainId, parseContext.chainId.equals("asd"));

        Assert.assertTrue("2>>>" + parseContext.head.nid, parseContext.head.nid.equals("A"));
        Assert.assertTrue("3>>>" + parseContext.head.next.size(), parseContext.head.next.size() == 4);
        Assert.assertTrue("4>>>" + parseContext.head.next.get(0), parseContext.head.next.get(0).nid .equals("B"));
        Assert.assertTrue("5>>>" + parseContext.head.next.get(1), parseContext.head.next.get(1).nid .equals("E"));
        printGraph(parseContext.head, new HashSet<>());

        System.out.println(parseContext.el);
    }

    @Test
    public void testFlowNodesParser2() throws Exception{
        String text = ResourceUtil.readUtf8Str("classpath://CASE2.md");
        Parser.ParseContext parseContext = new Parser.ParseContext(text);

        parsers.forEach(p ->p.parse(parseContext));


        Assert.assertTrue(">>>" + parseContext.head.nid, parseContext.head.nid.equals("A"));
        Assert.assertTrue(">>>" + parseContext.head.next.size(), parseContext.head.next.size() == 1);

        Parser.FlowChartNode b = parseContext.head.next.get(0);
        Assert.assertTrue(">>>" + b, b.nid.equals("B"));

        Parser.FlowChartNode c = b.next.get(0);
        Parser.FlowChartNode e = c.next.get(1);
        Assert.assertTrue(">>>" + e, e.next.get(0).nid .equals("B"));

        printGraph(parseContext.head, new HashSet<>());
    }

    @Test
    public void testFlowNodesParser3() throws Exception{
        String text = ResourceUtil.readUtf8Str("classpath://CASE3.md");
        Parser.ParseContext parseContext = new Parser.ParseContext(text);

        parsers.forEach(p ->p.parse(parseContext));

        Set<String> cache = new HashSet<>();
        printGraph(parseContext.head, cache);
        System.out.println(parseContext.el);
        Assert.assertTrue(">>>" + cache, !cache.contains("FB"));
        Assert.assertTrue(">>>" + cache, !cache.contains("FA"));
        Assert.assertTrue(">>>" + cache, !cache.contains("FABEND"));
    }

    private void printGraph(Parser.FlowChartNode head, Set<String> printedCache) {
        if (printedCache.contains(head.nid)) {
            return;
        }
        System.out.println(head);
        printedCache.add(head.nid);
        head.next.forEach(n -> printGraph(n, printedCache));


    }

}
