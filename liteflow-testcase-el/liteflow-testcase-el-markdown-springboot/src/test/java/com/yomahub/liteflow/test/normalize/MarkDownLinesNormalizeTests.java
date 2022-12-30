package com.yomahub.liteflow.test.normalize;

import com.yomahub.liteflow.parser.markdown.parser.ChainIdParser;
import com.yomahub.liteflow.parser.markdown.parser.FlowNodesParser;
import com.yomahub.liteflow.parser.markdown.parser.NormalizeParser;
import com.yomahub.liteflow.parser.markdown.parser.Parser;
import org.junit.Assert;
import org.junit.Test;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.util.List;

public class MarkDownLinesNormalizeTests {

    @Test
    public void testEmptyText() throws Exception{
        Parser.ParseContext parseContext = new Parser.ParseContext("");
        NormalizeParser normalizeParser = new NormalizeParser();

        normalizeParser.parse(parseContext);

        Assert.assertTrue(parseContext.lines.length == 0);
    }

    @Test
    public void testTitleOnly() throws Exception{
        Parser.ParseContext parseContext = new Parser.ParseContext("```mermaid\n" +
                "--- \r\n" +
                "   \n" +
                "\r\n" +
                "\n" +
                "title: asd    \n" +
                "   \n" +
                ";\n" +
                ";   \n" +
                "---\n```");
        NormalizeParser normalizeParser = new NormalizeParser();

        normalizeParser.parse(parseContext);

        Assert.assertTrue(">>" + parseContext.lines.length,parseContext.lines.length == 5);
        Assert.assertTrue(parseContext.lines[1].startsWith("---"));
        Assert.assertTrue(parseContext.lines[2].startsWith("title: "));
        Assert.assertTrue(parseContext.lines[3].startsWith("---"));
    }


    @Test
    public void testChainIdParser() throws Exception{
        Parser.ParseContext parseContext = new Parser.ParseContext("```mermaid\n" +
                "--- \r\n" +
                "   \n" +
                "\r\n" +
                "\n" +
                "title: asd   \n" +
                "\n" +
                "\n" +
                "---\n```");
        NormalizeParser normalizeParser = new NormalizeParser();

        normalizeParser.parse(parseContext);

        ChainIdParser chainIdParser = new ChainIdParser();

        chainIdParser.parse(parseContext);

        Assert.assertTrue(">>>" + parseContext.chainId, parseContext.chainId.equals("asd"));
    }

    @Test
    public void getScriptEngineFactory()
    {
        ScriptEngineManager manager = new ScriptEngineManager();
        List<ScriptEngineFactory> factories = manager.getEngineFactories();
        for (ScriptEngineFactory factory : factories)
        {
            System.out.println(factory.getNames());
        }
    }


}
