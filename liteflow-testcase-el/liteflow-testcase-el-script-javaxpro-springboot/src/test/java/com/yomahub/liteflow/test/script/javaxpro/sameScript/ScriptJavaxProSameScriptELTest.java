package com.yomahub.liteflow.test.script.javaxpro.sameScript;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

/**
 * 测试相同脚本内容但不同 nodeId 的场景
 * 应该都能正常加载和执行，不会被覆盖
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/sameScript/application.properties")
@SpringBootTest(classes = ScriptJavaxProSameScriptELTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.javaxpro.sameScript.cmp" })
public class ScriptJavaxProSameScriptELTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    /**
     * 测试相同脚本内容的两个不同 nodeId 都能正常执行
     * s1 和 s2 脚本内容完全相同，但应该都能被加载并执行
     */
    @Test
    public void testSameScriptDifferentNodeId() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response.getFirstContextBean();
        
        Assertions.assertTrue(response.isSuccess());
        
        // s1 应该能正常执行并写入数据
        Assertions.assertEquals("executed by s1", context.getData("s1"));
        
        // s2 应该能正常执行并写入数据（脚本内容和 s1 完全相同）
        Assertions.assertEquals("executed by s2", context.getData("s2"));
        
        // 验证执行步骤中包含 s1 和 s2
        String executeStepStr = response.getExecuteStepStr();
        Assertions.assertTrue(executeStepStr.contains("s1"));
        Assertions.assertTrue(executeStepStr.contains("s2"));
    }

    /**
     * 测试三个相同脚本内容的不同 nodeId 都能正常执行
     */
    @Test
    public void testSameScriptThreeNodeIds() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
        DefaultContext context = response.getFirstContextBean();
        
        Assertions.assertTrue(response.isSuccess());
        
        // 三个节点都应该能正常执行
        Assertions.assertEquals("data from a1", context.getData("a1"));
        Assertions.assertEquals("data from a2", context.getData("a2"));
        Assertions.assertEquals("data from a3", context.getData("a3"));
    }
}
