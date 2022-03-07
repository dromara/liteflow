package com.yomahub.liteflow.test.nullParam;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 单元测试:传递null param导致NPE的优化代码
 *
 * @author LeoLee
 * @since 2.6.6
 */
public class NullParamTest {

    private static FlowExecutor flowExecutor;

    @BeforeClass
    public static void init(){
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("nullParam/flow.xml");
        flowExecutor = FlowExecutorHolder.loadInstance(config);
    }

    /**
     * 支持无参的flow执行，以及param 为null时的异常抛出
     */
    @Test
    public void testNullParam() throws Exception {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1");
        Assert.assertTrue(response.isSuccess());
    }

}
