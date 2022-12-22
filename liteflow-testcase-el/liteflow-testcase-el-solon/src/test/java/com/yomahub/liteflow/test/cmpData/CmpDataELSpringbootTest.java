package com.yomahub.liteflow.test.cmpData;

import cn.hutool.core.date.DateUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.cmpData.vo.User;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.annotation.TestPropertySource;

/**
 * springboot环境EL常规的例子测试
 * @author Bryan.Zhang
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/cmpData/application.properties")
public class CmpDataELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    //最简单的情况
    @Test
    public void testCmpData() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        DefaultContext context = response.getFirstContextBean();
        User user = context.getData("user");
        Assert.assertEquals(27, user.getAge());
        Assert.assertEquals("jack", user.getName());
        Assert.assertEquals(0, user.getBirth().compareTo(DateUtil.parseDate("1995-10-01").toJdkDate()));
    }
}
