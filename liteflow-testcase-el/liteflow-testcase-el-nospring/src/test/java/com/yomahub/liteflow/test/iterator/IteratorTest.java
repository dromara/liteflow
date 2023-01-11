package com.yomahub.liteflow.test.iterator;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class IteratorTest extends BaseTest{

    private static FlowExecutor flowExecutor;

    @BeforeClass
    public static void init(){
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("iterator/flow.xml");
        flowExecutor = FlowExecutorHolder.loadInstance(config);
    }

    //最简单的情况
    @Test
    public void testIt1() throws Exception{
        List<String> list = ListUtil.toList("1","2","3");
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", list);
        Assert.assertTrue(response.isSuccess());
        DefaultContext context = response.getFirstContextBean();
        String str = context.getData("test");
        Assert.assertEquals("123", str);
    }

    //迭代器带break
    @Test
    public void testIt2() throws Exception{
        List<String> list = ListUtil.toList("1","2","3");
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", list);
        Assert.assertTrue(response.isSuccess());
        DefaultContext context = response.getFirstContextBean();
        String str = context.getData("test");
        Assert.assertEquals("12", str);
    }
}
