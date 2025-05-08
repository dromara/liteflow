package com.yomahub.liteflow.test.instanceIds;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.entity.InstanceInfoDto;
import com.yomahub.liteflow.flow.instanceId.NodeInstanceIdManageSpi;
import com.yomahub.liteflow.flow.instanceId.NodeInstanceIdManageSpiHolder;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.util.JsonUtil;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 测试生成 instanceId
 *
 * @author Jay li
 * @since 2.13.0
 */
@TestPropertySource(value = "classpath:/instanceIds/application.properties")
@SpringBootTest(classes = InstanceIdELSpringTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.instanceIds.cmp"})
public class InstanceIdELSpringTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    // 文件保存实例id
    @Test
    public void testInstanceIds1() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>a==>a==>a", response.getExecuteStepStr());

        String executeStepStrWithInstanceId = response.getExecuteStepStrWithInstanceId();
        Set<String> strings = extractValues(executeStepStrWithInstanceId);
        System.out.println(executeStepStrWithInstanceId);
        Assertions.assertEquals(strings.size(), 4);
    }

    // 重复调用实例id不变
    @Test
    public void testInstanceIds2() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>a==>a==>a", response.getExecuteStepStr());

        String executeStepStrWithInstanceId = response.getExecuteStepStrWithInstanceId();
        Set<String> set1 = extractValues(executeStepStrWithInstanceId);
        System.out.println(executeStepStrWithInstanceId);

        Assertions.assertEquals(set1.size(), 4);

        response = flowExecutor.execute2Resp("chain2", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>a==>a==>a", response.getExecuteStepStr());

        executeStepStrWithInstanceId = response.getExecuteStepStrWithInstanceId();
        Set<String> set2 = extractValues(executeStepStrWithInstanceId);
        System.out.println(executeStepStrWithInstanceId);

        Assertions.assertEquals(set2.size(), 4);
        Assertions.assertEquals(set1, set2);
    }


    @Test
    public void testInstanceIds3() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>a==>a==>a", response.getExecuteStepStr());

        String executeStepStrWithInstanceId = response.getExecuteStepStrWithInstanceId();
        List<String> strings = extractValuesList(executeStepStrWithInstanceId);
        NodeInstanceIdManageSpi nodeInstanceIdManageSpi = NodeInstanceIdManageSpiHolder.getInstance().getNodeInstanceIdManageSpi();

        for (int i = 0; i < strings.size(); i++) {
            Assertions.assertEquals(nodeInstanceIdManageSpi.getNodeLocationById("chain2", strings.get(i)), i);
        }

        System.out.println(executeStepStrWithInstanceId);
        Assertions.assertEquals(strings.size(), 4);
    }

    public static Set<String> extractValues(String input) {
        Set<String> values = new HashSet<>();
        Pattern pattern = Pattern.compile("\\[(.*?)]");
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }

    public static List<String> extractValuesList(String input) {
        List<String> values = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[(.*?)]");
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }


    // chain3 if 脚本
    @Test
    public void testXmlChain3() {
        String chain4InstanceStr = queryInstanceStrByChainId("chain3");
        LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");

        Assertions.assertEquals("x1==>a==>b", response.getExecuteStepStr());
        System.out.println(chain4InstanceStr);
        Assertions.assertEquals(chain4InstanceStr, response.getExecuteStepStrWithInstanceId());
        List<String> extractStrings = extractValuesList(chain4InstanceStr);
        Assertions.assertEquals(Sets.newHashSet(extractStrings).size(), 3);
    }


    // chain5 switch 切换 for 表达式
    @Test
    public void testXmlChain5() {
        String chainId = "chain5";

        LiteflowResponse response = flowExecutor.execute2Resp(chainId, "arg");
        String executeStepStr = response.getExecuteStepStr();
        Assertions.assertEquals("e==>c", response.getExecuteStepStr());

        String instancePath = constructInstancePath(executeStepStr, chainId);
        Assertions.assertEquals(instancePath, response.getExecuteStepStrWithInstanceId());
        List<String> extractStrings = extractValuesList(instancePath);
        Assertions.assertEquals(Sets.newHashSet(extractStrings).size(), 2);
    }

    //  FOR(x).DO(CATCH(THEN(a,b,a)));
    @Test
    public void testXmlChain4() {
        String chainId = "chain4";
        LiteflowResponse response = flowExecutor.execute2Resp(chainId, "arg");
        String chain4InstanceStr2 = queryInstanceStrByChainId(chainId);
        String executeStepStr = response.getExecuteStepStr();
        Assertions.assertEquals("x==>a==>b==>a", executeStepStr);

        String instancePath = constructInstancePath(executeStepStr, chainId);
        Assertions.assertEquals(instancePath, response.getExecuteStepStrWithInstanceId());

        List<String> extractStrings = extractValuesList(chain4InstanceStr2);
        Assertions.assertEquals(Sets.newHashSet(extractStrings).size(), 4);
    }


    // THEN(a,WHEN(b, c), a)
    @Test
    public void testXmlChain6() {
        String chainId = "chain6";

        LiteflowResponse response = flowExecutor.execute2Resp(chainId, "arg");
        String executeStepStr = response.getExecuteStepStr();
        Assertions.assertTrue( response.isSuccess());
        String instancePath = constructInstancePath(executeStepStr, chainId);
        Assertions.assertEquals(instancePath, response.getExecuteStepStrWithInstanceId());
        List<String> extractStrings = extractValuesList(instancePath);
        Assertions.assertEquals(Sets.newHashSet(extractStrings).size(), 4);
    }


    // CATCH(THEN(a,b)).DO(c)
    @Test
    public void testXmlChain7() {
        String chainId = "chain7";

        LiteflowResponse response = flowExecutor.execute2Resp(chainId, "arg");
        String executeStepStr = response.getExecuteStepStr();
        Assertions.assertEquals("a==>b", response.getExecuteStepStr());

        String instancePath = constructInstancePath(executeStepStr, chainId);
        Assertions.assertEquals(instancePath, response.getExecuteStepStrWithInstanceId());
        List<String> extractStrings = extractValuesList(instancePath);
        Assertions.assertEquals(Sets.newHashSet(extractStrings).size(), 2);
    }

    @Test
    public void getNodeByIdAndInstanceIdTest() {
        String[] chainIds = new String[]{"chain1", "chain2", "chain3", "chain4", "chain5", "chain6", "chain7"};
        for (String chainId : chainIds) {
            LiteflowResponse response = flowExecutor.execute2Resp(chainId, "arg");
            String executeStepStrWithInstanceId = response.getExecuteStepStrWithInstanceId();

            Map<String, String> instanceMap = extractKeyValuePairs(executeStepStrWithInstanceId);

            NodeInstanceIdManageSpi nodeInstanceIdManageSpi = NodeInstanceIdManageSpiHolder.getInstance().getNodeInstanceIdManageSpi();
            for (Map.Entry<String, String> entry : instanceMap.entrySet()) {
                Node node = nodeInstanceIdManageSpi.getNodeByIdAndInstanceId(chainId, entry.getKey());
                Assertions.assertEquals(node.getId(), entry.getValue());
            }
        }

    }


    @Test
    public void getNodeByIdAndIndexTest() {
        String[] chainIds = new String[]{"chain1", "chain2", "chain3", "chain4", "chain5", "chain6", "chain7"};
        for (String chainId : chainIds) {
            LiteflowResponse response = flowExecutor.execute2Resp(chainId, "arg");
            String executeStepStrWithInstanceId = response.getExecuteStepStrWithInstanceId();

            Map<String, String> instanceMap = extractKeyValuePairs(executeStepStrWithInstanceId);

            Map<String, Integer> idCntMap = new HashMap<>();

            NodeInstanceIdManageSpi nodeInstanceIdManageSpi = NodeInstanceIdManageSpiHolder.getInstance().getNodeInstanceIdManageSpi();
            for (Map.Entry<String, String> entry : instanceMap.entrySet()) {
                idCntMap.put(entry.getValue(), idCntMap.getOrDefault(entry.getValue(), -1) + 1);
                Node node = nodeInstanceIdManageSpi.getNodeByIdAndIndex(chainId, entry.getValue(), idCntMap.get(entry.getValue()));
                Assertions.assertEquals(node.getId(), entry.getValue());
            }
        }
    }

    @Test
    public void getNodeInstanceIdsTest() {
        String[] chainIds = new String[]{"chain1", "chain2", "chain3", "chain4", "chain5", "chain6", "chain7"};
        for (String chainId : chainIds) {
            LiteflowResponse response = flowExecutor.execute2Resp(chainId, "arg");
            String executeStepStrWithInstanceId = response.getExecuteStepStrWithInstanceId();

            Map<String, List<String>> instanceMap = extractKeyValues(executeStepStrWithInstanceId);

            NodeInstanceIdManageSpi nodeInstanceIdManageSpi = NodeInstanceIdManageSpiHolder.getInstance().getNodeInstanceIdManageSpi();
            for (Map.Entry<String, List<String>> entry : instanceMap.entrySet()) {
                Assertions.assertEquals(entry.getValue(), nodeInstanceIdManageSpi.getNodeInstanceIds(chainId, entry.getKey()));
            }
        }

    }


    private String constructInstancePath(String executeStepStr, String chainId) {
        Map<String, InstanceInfoDto> instanceMap = queryInstanceMapByChainId(chainId);
        String[] nodes = executeStepStr.split("==>");

        StringBuilder nodePathStr = new StringBuilder();
        Map<String, Integer> tmpMap = new HashMap<>();
        for (String node : nodes) {
            tmpMap.put(node, tmpMap.getOrDefault(node, -1) + 1);
            nodePathStr.append("==>").append(node).append("[")
                    .append(instanceMap.get(node + "_" + tmpMap.get(node)).getInstanceId())
                    .append("]");
        }

        return nodePathStr.toString().replaceFirst("==>", "");

    }


    private String queryInstanceStrByChainId(String chainId) {
        String instanceId = queryInstanceIdInfo(chainId);
        // 解析 JSON
        List<InstanceInfoDto> instanceInfoDtos = JsonUtil.parseList(instanceId, InstanceInfoDto.class);
        // 构造实例id字符串
        StringBuilder result = new StringBuilder();
        int i = 0;

        for (InstanceInfoDto dto : instanceInfoDtos) {
            result.append(dto.getNodeId()).append("[").append(dto.getInstanceId()).append("]");
            if (i + 1 < instanceInfoDtos.size()) {
                result.append("==>");
            }
            i++;
        }

        return result.toString();
    }

    // key 为 nodeId_index
    private Map<String, InstanceInfoDto> queryInstanceMapByChainId(String chainId) {
        // 查询数据库实例id
        String instanceId = queryInstanceIdInfo(chainId);
        // 解析 JSON
        List<InstanceInfoDto> instanceInfos = JsonUtil.parseList(instanceId, InstanceInfoDto.class);
        // 构造实例id字符串
        Map<String, InstanceInfoDto> result = new HashMap<>();
        instanceInfos.forEach(instanceInfo -> result.put(instanceInfo.getNodeId() + "_" + instanceInfo.getIndex(), instanceInfo));

        return result;

    }


    /**
     * key 为 InstanceId  value 为 nodeId
     */
    private Map<String, String> extractKeyValuePairs(String input) {
        String[] parts = input.split("==>");

        // 创建一个 Map 来存储结果
        Map<String, String> resultMap = new HashMap<>();

        for (String part : parts) {
            // 去掉前后括号
            int startIndex = part.indexOf('[');
            int endIndex = part.lastIndexOf(']');

            if (startIndex != -1 && endIndex != -1) {
                String value = part.substring(0, startIndex).trim();
                String key = part.substring(startIndex + 1, endIndex).trim();

                // 将键值对放入 Map
                resultMap.put(key, value);
            }
        }
        return resultMap;
    }


    /**
     * key 为 nodeId value 为 list InstanceId
     */
    private Map<String, List<String>> extractKeyValues(String input) {
        String[] parts = input.split("==>");

        // 创建一个 Map 来存储结果
        Map<String, List<String>> resultMap = new HashMap<>();

        for (String part : parts) {
            // 去掉前后括号
            int startIndex = part.indexOf('[');
            int endIndex = part.lastIndexOf(']');

            if (startIndex != -1 && endIndex != -1) {
                String key = part.substring(0, startIndex).trim();
                String value = part.substring(startIndex + 1, endIndex).trim();

                List<String> mapOrDefault = resultMap.getOrDefault(key, new ArrayList<>());
                mapOrDefault.add(value);
                resultMap.put(key, mapOrDefault);
            }
        }
        return resultMap;
    }


    public String queryInstanceIdInfo(String chainId) {
        NodeInstanceIdManageSpi nodeInstanceIdManageSpi = NodeInstanceIdManageSpiHolder.getInstance().getNodeInstanceIdManageSpi();
        List<String> readInstanceIdFiles = nodeInstanceIdManageSpi.readInstanceIdFile(chainId);

        return readInstanceIdFiles.get(1);
    }
}