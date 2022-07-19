package com.yomahub.liteflow.test.absoluteConfigPath;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * spring环境下，规则配置文件通过绝对路径获取
 * @author zendwang
 * @since 2.8.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/absoluteConfigPath/application.xml")
public class AbsoluteConfigPathELSpringTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testAbsoluteConfig(){
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
    }
}
