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
/*@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/deadLoopChain/application.properties")
@SpringBootTest(classes = DeadLoopChainSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.deadLoopChain.cmp"})*/
public class DeadLoopChainSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //死循环问题解析时自动发现，抛错
    //为了写测试用例，才配置了liteflow.parse-on-start=false参数，实际上应用不用配置延迟加载参数
    //自从2.6.8之后，支持循环依赖，但是用户必须在组件里自己判断退出的条件，否则会报栈溢出
    //所以这个测试用例暂时不打开
    //为什么不删除呢？是因为如果用户不自己判断退出的条件。会报出栈溢出。以后希望liteflow自己能抛出相关的错。而不是抛出JDK的异常。所以暂时留着。
    //@Test(expected = CyclicDependencyException.class)
    public void testDeadLoopChain() {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1", "arg");
    }
}
