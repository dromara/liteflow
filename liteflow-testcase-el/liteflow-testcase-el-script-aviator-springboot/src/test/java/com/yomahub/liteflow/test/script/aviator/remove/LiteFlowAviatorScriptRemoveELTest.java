package com.yomahub.liteflow.test.script.aviator.remove;

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
 * 测试脚本的卸载和重载功能
 *
 * @author DaleLee
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/remove/application.properties")
@SpringBootTest(classes = LiteFlowAviatorScriptRemoveELTest.class)
@EnableAutoConfiguration
public class LiteFlowAviatorScriptRemoveELTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    private ScriptExecutor scriptExecutor = ScriptExecutorFactory.loadInstance()
            .getScriptExecutor(ScriptTypeEnum.AVIATOR.getDisplayName());

    // 仅卸载脚本
    @Test
    public void testUnload() {
        flowExecutor.reloadRule();

        // 获取节点id
        List<String> nodeIds = scriptExecutor.getNodeIds();
        Assertions.assertEquals(2, nodeIds.size());
        Assertions.assertTrue(nodeIds.contains("s1"));
        Assertions.assertTrue(nodeIds.contains("s2"));

        // 保证脚本可以正常运行
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertEquals(Long.valueOf(6), context.getData("s1"));

        // 卸载脚本
        scriptExecutor.unLoad("s1");
        response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertEquals(ScriptLoadException.class, response.getCause().getClass());
        Assertions.assertEquals("script for node[s1] is not loaded", response.getMessage());

        // 脚本已卸载
        Assertions.assertFalse(scriptExecutor.getNodeIds().contains("s1"));
    }

    // 卸载节点和脚本
    @Test
    public void testRemove() {
        flowExecutor.reloadRule();

        // 保证脚本可以正常运行
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
        Assertions.assertTrue(response.isSuccess());
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertEquals(Long.valueOf(5), context.getData("s2"));

        // 卸载节点
        FlowBus.unloadScriptNode("s2");

        // 旧 chain 报脚本加载错误
        response = flowExecutor.execute2Resp("chain2", "arg");
        Assertions.assertEquals(ScriptLoadException.class, response.getCause().getClass());

        // 新 chian 会找不到节点
        Assertions.assertThrows(ELParseException.class,
                () -> LiteFlowChainELBuilder.createChain()
                        .setChainId("chain3")
                        .setEL("THEN(s2)")
                        .build());

        // 节点已卸载
        Assertions.assertFalse(FlowBus.containNode("s2"));
        // 脚本已卸载
        Assertions.assertFalse(scriptExecutor.getNodeIds().contains("s2"));
    }

    // 重载脚本
    @Test
    public void testReloadScript() {
        flowExecutor.reloadRule();
        String script = "setData(defaultContext,\"s1\",\"abc\");";
        FlowBus.reloadScript("s1", script);
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response.getFirstContextBean();
        // 执行结果变更
        Assertions.assertEquals("abc", context.getData("s1"));
        // 脚本变更
        Assertions.assertEquals(FlowBus.getNode("s1").getScript(), script);
    }
}
