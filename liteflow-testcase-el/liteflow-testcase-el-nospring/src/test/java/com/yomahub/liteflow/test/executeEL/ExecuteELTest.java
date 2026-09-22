package com.yomahub.liteflow.test.executeEL;

import cn.hutool.crypto.digest.MD5;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.util.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 动态 EL 原文执行与缓存复用的回归测试（#IKH1YA）。
 */
public class ExecuteELTest extends BaseTest {

    private static FlowExecutor flowExecutor;

    @BeforeAll
    public static void init() {
        LiteflowConfig config = new LiteflowConfig();
        config.setPrintBanner(false);
        config.setPrintExecutionLog(false);
        flowExecutor = new FlowExecutor(config);
    }

    @BeforeEach
    public void registerNode() {
        FlowBus.cleanCache();
        LiteFlowNodeBuilder.createCommonNode().setId("a").setClazz(CaptureCmp.class).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "select id from t where name = 'bob'",
            "{\"sql\":\"name = 'bob'\",\"label\":\"hello world\"}",
            "O'Reilly says \"hello\"",
            "C:\\rules\\file.txt",
            "line 1\tline 2\r\nline 3",
            "https://example.com/a b // text /* text */;",
            "中文 内容"
    })
    public void testPreservesStringLiterals(String value) {
        String literal = JsonUtil.toJsonString(value);
        String el = "THEN(a.data(" + literal + ").tag(" + literal + "));";

        LiteflowResponse response = flowExecutor.execute2RespWithEL(el);

        assertValues(response, value, value);
        assertOriginalEl(response, el);
    }

    @Test
    public void testPreservesLiteralWhitespace() {
        String value = "first line\tsecond line\nthird line\r\nlast line";
        String el = "THEN(a.data(\"" + value + "\").tag(\"" + value + "\"));";

        LiteflowResponse response = flowExecutor.execute2RespWithEL(el);

        assertValues(response, value, value);
        assertOriginalEl(response, el);
    }

    @Test
    public void testPreservesLineCommentBoundary() {
        String el = "// keep this newline\nTHEN(a.data(\"hello world\").tag(\"comment\"));";

        LiteflowResponse response = flowExecutor.execute2RespWithEL(el);

        assertValues(response, "hello world", "comment");
        assertOriginalEl(response, el);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testDifferentLiteralValuesDoNotShareChain(boolean reverse) {
        String firstValue = reverse ? "ab" : "a b";
        String secondValue = reverse ? "a b" : "ab";
        for (String modifier : new String[]{"data", "tag"}) {
            String firstEl = "THEN(a." + modifier + "(\"" + firstValue + "\"));";
            String secondEl = "THEN(a." + modifier + "(\"" + secondValue + "\"));";

            LiteflowResponse first = flowExecutor.execute2RespWithEL(firstEl);
            assertValues(first, "data".equals(modifier) ? firstValue : null,
                    "tag".equals(modifier) ? firstValue : null);

            LiteflowResponse second = flowExecutor.execute2RespWithEL(secondEl);
            assertValues(second, "data".equals(modifier) ? secondValue : null,
                    "tag".equals(modifier) ? secondValue : null);
            Assertions.assertNotEquals(first.getChainId(), second.getChainId());

            LiteflowResponse repeated = flowExecutor.execute2RespWithEL(firstEl);
            assertValues(repeated, "data".equals(modifier) ? firstValue : null,
                    "tag".equals(modifier) ? firstValue : null);
            Assertions.assertEquals(first.getChainId(), repeated.getChainId());
        }
    }

    @Test
    public void testSameOriginalElReusesChainAcrossOverloads() {
        String el = "THEN(a.data(\"name = 'bob'\").tag(\"hello world\"));";
        LiteflowResponse first = flowExecutor.execute2RespWithEL(el);
        assertValues(first, "name = 'bob'", "hello world");

        LiteflowResponse withParam = flowExecutor.execute2RespWithEL(el, "param");
        assertValues(withParam, "name = 'bob'", "hello world");
        Assertions.assertEquals(first.getChainId(), withParam.getChainId());

        LiteflowResponse withClass = flowExecutor.execute2RespWithEL(el, "param", "class-rid",
                DefaultContext.class);
        assertValues(withClass, "name = 'bob'", "hello world");
        Assertions.assertEquals(first.getChainId(), withClass.getChainId());
        Assertions.assertEquals("class-rid", withClass.getRequestId());

        DefaultContext context = new DefaultContext();
        LiteflowResponse withBean = flowExecutor.execute2RespWithEL(el, "param", "bean-rid", context);
        assertValues(withBean, "name = 'bob'", "hello world");
        Assertions.assertSame(context, withBean.getFirstContextBean());
        Assertions.assertEquals(first.getChainId(), withBean.getChainId());
        Assertions.assertEquals("bean-rid", withBean.getRequestId());
        Assertions.assertEquals(1, FlowBus.getChainMap().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            " THEN( a.tag(\"same\") ); ",
            "THEN(\na.tag(\"same\")\n);",
            "THEN(a.tag(\"same\"));;",
            "THEN(a.tag('same'));"
    })
    public void testDifferentSourceTextDoesNotShareChain(String el) {
        LiteflowResponse first = flowExecutor.execute2RespWithEL("THEN(a.tag(\"same\"));");
        assertValues(first, null, "same");

        LiteflowResponse second = flowExecutor.execute2RespWithEL(el);
        assertValues(second, null, "same");
        assertOriginalEl(second, el);
        Assertions.assertNotEquals(first.getChainId(), second.getChainId());
    }

    @Test
    public void testReusesBuilderChainOnlyForIdenticalSource() {
        String el = "THEN(a.data(\"a b\").tag(\"builder\"));";
        LiteFlowChainELBuilder.createChain().setChainId("builderChain").setEL(el).build();

        LiteflowResponse same = flowExecutor.execute2RespWithEL(el);
        assertValues(same, "a b", "builder");
        assertOriginalEl(same, el);
        Assertions.assertEquals("builderChain", same.getChainId());

        LiteflowResponse different = flowExecutor.execute2RespWithEL(
                "THEN(a.data(\"ab\").tag(\"builder\"));");
        assertValues(different, "ab", "builder");
        Assertions.assertNotEquals("builderChain", different.getChainId());
    }

    @Test
    public void testRemovingChainRemovesOriginalElMapping() {
        String el = "THEN(a.data(\"hello world\"));";
        LiteflowResponse first = flowExecutor.execute2RespWithEL(el);
        assertValues(first, "hello world", null);
        String md5 = MD5.create().digestHex(el);
        Assertions.assertEquals(first.getChainId(), FlowBus.getChainIdByElMd5(md5));

        Assertions.assertTrue(FlowBus.removeChain(first.getChainId()));
        Assertions.assertNull(FlowBus.getChainIdByElMd5(md5));

        LiteflowResponse second = flowExecutor.execute2RespWithEL(el);
        assertValues(second, "hello world", null);
        Assertions.assertNotEquals(first.getChainId(), second.getChainId());
    }

    private static void assertOriginalEl(LiteflowResponse response, String el) {
        Assertions.assertEquals(el, FlowBus.getChain(response.getChainId()).getEl());
        Assertions.assertEquals(MD5.create().digestHex(el),
                FlowBus.getChain(response.getChainId()).getElMd5());
    }

    private static void assertValues(LiteflowResponse response, String data, String tag) {
        Assertions.assertTrue(response.isSuccess(), response.getMessage());
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertEquals(data, context.getData("data"));
        Assertions.assertEquals(tag, context.getData("tag"));
    }

    public static class CaptureCmp extends NodeComponent {
        @Override
        public void process() {
            DefaultContext context = getFirstContextBean();
            String data = getCmpData(String.class);
            if (data != null) {
                context.setData("data", data);
            }
            if (getTag() != null) {
                context.setData("tag", getTag());
            }
        }
    }
}
