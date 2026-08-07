package com.yomahub.liteflow.test.script.javaxpro.refnode;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.ScriptExecutorFactory;
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
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Stack;

/**
 * 验证 javax-pro(liquor) 编译出的脚本组件上的 refNodeStackTL 不会无限增长
 * <p>
 * 场景：预创建有限条 chain（各含同一 java 脚本节点），同一线程轮询执行不同 chain。
 * EL 构建时每个节点出现位置都会 clone 出一个新的 Node 对象，JavaxProExecutor.getExecutableCmp
 * 对编译出的 NodeComponent 调用 setRefNode（仅与栈顶做引用相等比较）却从不 removeRefNode，
 * 导致编译组件上的 refNodeStackTL 中的 Stack 随调用次数线性增长（内存泄漏）。
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/refnode/application.properties")
@SpringBootTest(classes = RefNodeStackLeakTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.javaxpro.refnode.cmp" })
public class RefNodeStackLeakTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testRefNodeStackNotLeak() throws Exception {
        // 同一线程轮询执行两条 chain（两条 chain 中的 s1 是不同的 Node 克隆对象）
        for (int i = 0; i < 100; i++) {
            LiteflowResponse respA = flowExecutor.execute2Resp("chainA", "arg");
            Assertions.assertTrue(respA.isSuccess());
            LiteflowResponse respB = flowExecutor.execute2Resp("chainB", "arg");
            Assertions.assertTrue(respB.isSuccess());
        }

        Stack<?> refNodeStack = getRefNodeStack("s1");

        // 执行结束后，编译组件的 refNode 栈应当被清理干净（ThreadLocal 被 remove 或栈为空）
        // 泄漏情况下这里会随执行次数线性增长（本用例中为 200）
        Assertions.assertTrue(refNodeStack == null || refNodeStack.isEmpty(),
                "refNodeStackTL leaked, size=" + (refNodeStack == null ? 0 : refNodeStack.size()));
    }

    /**
     * 脚本抛异常时，refNode 同样要被清理（清理动作放在 finally 中的意义）
     */
    @Test
    public void testRefNodeStackNotLeakOnScriptError() throws Exception {
        for (int i = 0; i < 50; i++) {
            Assertions.assertFalse(flowExecutor.execute2Resp("errChainA", "arg").isSuccess());
            Assertions.assertFalse(flowExecutor.execute2Resp("errChainB", "arg").isSuccess());
        }

        Stack<?> refNodeStack = getRefNodeStack("s2");

        Assertions.assertTrue(refNodeStack == null || refNodeStack.isEmpty(),
                "refNodeStackTL leaked on error path, size=" + (refNodeStack == null ? 0 : refNodeStack.size()));
    }

    @SuppressWarnings("unchecked")
    private Stack<?> getRefNodeStack(String nodeId) throws Exception {
        ScriptExecutor scriptExecutor = ScriptExecutorFactory.loadInstance().getScriptExecutor("java");

        Field mapField = scriptExecutor.getClass().getDeclaredField("compiledScriptMap");
        mapField.setAccessible(true);
        Map<String, NodeComponent> compiledScriptMap = (Map<String, NodeComponent>) mapField.get(scriptExecutor);
        NodeComponent cmp = compiledScriptMap.get(nodeId);
        Assertions.assertNotNull(cmp, "compiled script component not found for node " + nodeId);

        Field tlField = NodeComponent.class.getDeclaredField("refNodeStackTL");
        tlField.setAccessible(true);
        ThreadLocal<Stack<?>> tl = (ThreadLocal<Stack<?>>) tlField.get(cmp);
        return tl.get();
    }
}
