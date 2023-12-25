package com.yomahub.liteflow.test.script.graaljs.remove;

import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.ScriptExecutorFactory;
import com.yomahub.liteflow.script.exception.ScriptLoadException;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.util.List;

/**
 * 测试脚本的卸载功能
 *
 * @author DaleLee
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/remove/application.properties")
@SpringBootTest(classes = LiteFlowJsScriptRemoveELTest.class)
@EnableAutoConfiguration
public class LiteFlowJsScriptRemoveELTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testUnload() {
        ScriptExecutor scriptExecutor = ScriptExecutorFactory.loadInstance()
                .getScriptExecutor(ScriptTypeEnum.JS.getDisplayName());

        // 获取节点id
        List<String> nodeIds = scriptExecutor.getNodeIds();
        Assertions.assertEquals(nodeIds.size(), 2);
        Assertions.assertTrue(nodeIds.contains("s1"));
        Assertions.assertTrue(nodeIds.contains("s2"));

        // 保证脚本可以正常运行
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertEquals(Integer.valueOf(6), context.getData("s1"));

        // 卸载脚本
        scriptExecutor.unLoad("s1");
        response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertEquals(ScriptLoadException.class, response.getCause().getClass());
        Assertions.assertEquals("script for node[s1] is not loaded", response.getMessage());

        // 脚本已卸载
        Assertions.assertFalse(scriptExecutor.getNodeIds().contains("s1"));
        // 节点已卸载
        Assertions.assertFalse(FlowBus.containNode("s1"));
        Assertions.assertFalse(FlowBus.removeNode("s1"));
    }
}
