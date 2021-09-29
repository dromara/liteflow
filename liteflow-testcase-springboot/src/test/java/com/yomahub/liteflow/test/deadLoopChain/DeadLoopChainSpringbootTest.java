package com.yomahub.liteflow.test.deadLoopChain;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.exception.CyclicDependencyException;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;


/**
 * 测试springboot下循环chain死循环问题
 * @author Bryan.Zhang
 * @since 2.5.10
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/deadLoopChain/application.properties")
@SpringBootTest(classes = DeadLoopChainSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.deadLoopChain.cmp"})
public class DeadLoopChainSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //死循环问题解析时自动发现，抛错
    //为了写测试用例，才配置了liteflow.parse-on-start=false参数，实际上应用不用配置延迟加载参数
    @Test(expected = CyclicDependencyException.class)
    public void testDeadLoopChain() {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1", "arg");
    }
}
