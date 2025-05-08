package com.yomahub.liteflow.test.script.graaljs.getnodes;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.meta.LiteflowMetaOperator;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 根据 chainId 获取节点测试
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/getnodes/application.properties")
@SpringBootTest(classes = LiteFlowScriptGetNodesGraaljsELTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.script.graaljs.getnodes.cmp")
public class LiteFlowScriptGetNodesGraaljsELTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void getNodesTest1() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());
        List<Node> nodes = LiteflowMetaOperator.getNodes("chain1");
        // 判断数量
        Assertions.assertEquals(5, nodes.size());
        // 判断 id
        List<String> nodeIds = nodes.stream().map(Node::getId)
                .collect(Collectors.toList());
        List<String> targetIds = Arrays.asList("a", "b", "c", "s1", "s2");
        for (String id : targetIds) {
            Assertions.assertTrue(nodeIds.contains(id));
        }
    }

    @Test
    public void getNodesTest2() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
        Assertions.assertTrue(response.isSuccess());
        List<Node> nodes = LiteflowMetaOperator.getNodes("chain2");
        // 判断总数量
        Assertions.assertEquals(6, nodes.size());
        // 判断 id 与数量
        List<String> nodeIds = nodes.stream().map(Node::getId)
                .collect(Collectors.toList());
        Map<String, Integer> map = listToMap(nodeIds);
        Map<String, Integer> targetMap = new HashMap<>();
        targetMap.put("a", 3);
        targetMap.put("b", 1);
        targetMap.put("s1", 2);
        Assertions.assertTrue(targetMap.equals(map));
        // 判断 tag
        List<String> nodeTags = nodes.stream().map(Node::getTag)
                .collect(Collectors.toList());
        List<String> targetTags = Arrays.asList("a1", "a2", "a3", "b1", "s11", "s12");
        for (String id : targetTags) {
            Assertions.assertTrue(nodeTags.contains(id));
        }
    }

    @Test
    public void getNodesTest3() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
        Assertions.assertTrue(response.isSuccess());
        List<Node> nodes = LiteflowMetaOperator.getNodes("chain3");
        // 判断总数量
        Assertions.assertEquals(8, nodes.size());
        // 判断 id 与数量
        List<String> nodeIds = nodes.stream().map(Node::getId)
                .collect(Collectors.toList());
        Map<String, Integer> map = listToMap(nodeIds);
        Map<String, Integer> targetMap = new HashMap<>();
        targetMap.put("a", 2);
        targetMap.put("b", 2);
        targetMap.put("c", 1);
        targetMap.put("f", 1);
        targetMap.put("s1", 1);
        targetMap.put("s2", 1);
        Assertions.assertTrue(targetMap.equals(map));
    }

    @Test
    public void getNodesTest4() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
        Assertions.assertTrue(response.isSuccess());
        List<Node> nodes = LiteflowMetaOperator.getNodes("chain4");
        // 判断数量
        Assertions.assertEquals(5, nodes.size());
        // 判断 id
        List<String> nodeIds = nodes.stream().map(Node::getId)
                .collect(Collectors.toList());
        List<String> targetIds = Arrays.asList("a", "b", "c", "s");
        for (String id : targetIds) {
            Assertions.assertTrue(nodeIds.contains(id));
        }
    }

    @Test
    public void getNodesTest5() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
        Assertions.assertTrue(response.isSuccess());
        List<Node> nodes = LiteflowMetaOperator.getNodes("chain5");
        // 判断数量
        Assertions.assertEquals(3, nodes.size());
        // 判断 id
        List<String> nodeIds = nodes.stream().map(Node::getId)
                .collect(Collectors.toList());
        List<String> targetIds = Arrays.asList("x", "y", "s1");
        for (String id : targetIds) {
            Assertions.assertTrue(nodeIds.contains(id));
        }
    }

    // 统计节点 id 出现的数量
    private Map<String, Integer> listToMap(List<String> list) {
        Map<String, Integer> map = new HashMap<>();
        for (String s : list) {
            map.put(s, map.getOrDefault(s, 0) + 1);
        }
        return map;
    }
}
