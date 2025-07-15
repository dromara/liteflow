package com.yomahub.liteflow.test.redis;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.parser.helper.NodeConvertHelper;
import com.yomahub.liteflow.parser.redis.mode.RClient;
import com.yomahub.liteflow.parser.redis.mode.RedisParserHelper;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * springboot环境下的redis 集群配置源订阅模式功能测试
 * <p>
 * 测试用例会在1号database中添加测试数据 chainKey:testChainKey; scriptKey:testScriptKey
 * 测试完成后清除测试数据
 *
 * @author jay li
 * @since 2.13.3
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/redis/application-sub-cluster-xml.properties")
@SpringBootTest(classes = RedisClusterSubscribeSpringBootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.redis.cmp"})
public class RedisClusterSubscribeSpringBootTest extends BaseTest {
    @Mock
    private RedissonClient redissonClient;

    @Resource
    private FlowExecutor flowExecutor;

    @MockBean(name = "chainClient")
    private  RClient chainClient;

    @MockBean(name = "scriptClient")
    private  RClient scriptClient;

    @Mock
    private RMapCache chainKey;

    @Mock
    private RMapCache scriptKey;

    @BeforeEach
    public void setUpBeforeClass() {

        when(redissonClient.getMapCache("testChainKey")).thenReturn(chainKey);
        when(redissonClient.getMapCache("testScriptKey")).thenReturn(scriptKey);

        when(scriptKey.get("s1:script:脚本s1:groovy")).thenReturn("defaultContext.setData(\"test1\",\"hello s1\");");
        when(scriptKey.get("s2:script:脚本s2:js")).thenReturn("defaultContext.setData(\"test2\",\"hello s2\");");
        when(scriptKey.get("s3:script:脚本s3")).thenReturn("defaultContext.setData(\"test3\",\"hello s3\");");

        Set<Map.Entry<Object, Object>> mockEntrySet = new HashSet<>();
        mockEntrySet.add(createMockEntry("s1:script:脚本s1:groovy", "defaultContext.setData(\"test1\",\"hello s1\");"));
        mockEntrySet.add(createMockEntry("s2:script:脚本s2:js", "defaultContext.setData(\"test2\",\"hello s2\");"));
        mockEntrySet.add(createMockEntry("s3:script:脚本s3", "defaultContext.setData(\"test3\",\"hello s3\");"));
        when(scriptKey.entrySet()).thenReturn(mockEntrySet);

        when(chainKey.get("chain1")).thenReturn("THEN(a, b, c);");
        when(chainKey.get("chain2")).thenReturn("THEN(a, b, c, s3);");
        when(chainKey.get("chain3")).thenReturn("THEN(a, b, c, s1, s2);");

        mockEntrySet = new HashSet<>();
        mockEntrySet.add(createMockEntry("chain1", "THEN(a, b, c);"));
        mockEntrySet.add(createMockEntry("chain2", "THEN(a, b, c, s3);"));
        mockEntrySet.add(createMockEntry("chain3", "THEN(a, b, c, s1, s2);"));

        Set<Map.Entry<Object, Object>> mockEntrySet1 = new HashSet<>(mockEntrySet);

        mockEntrySet1.add(createMockEntry("chain1", "THEN(a, c, b);"));
        when(chainKey.entrySet()).thenReturn(mockEntrySet).thenReturn(mockEntrySet1);

        when(chainClient.getMap(anyString())).thenReturn(chainKey);
        when(scriptClient.getMap(anyString())).thenReturn(scriptKey);
    }

    private Map.Entry<Object, Object> createMockEntry(Object key, Object value) {
        Map.Entry<Object, Object> entry = mock(Map.Entry.class);
        when(entry.getKey()).thenReturn(key);
        when(entry.getValue()).thenReturn(value);
        return entry;
    }

    /**
     * 测试chain
     */
    @Test
    public void testSubWithXml() throws InterruptedException {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());

        //修改redis中规则
        changeXMLData();
        //重新加载规则
        Thread.sleep(100);

        Assertions.assertEquals("a==>c==>b", flowExecutor.execute2Resp("chain1", "arg").getExecuteStepStr());

        //删除redis中规则
        deleteXMLData();
        //重新加载规则
        Thread.sleep(100);
        //由于chain1已被删除 这里会报ChainNotFoundException异常
        response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(!response.isSuccess());

        //添加redis中规则
        addXMLData();
        //重新加载规则
        Thread.sleep(100);
        Assertions.assertEquals("b==>c", flowExecutor.execute2Resp("chain4", "arg").getExecuteStepStr());
    }

    /**
     * 测试script
     */
    @Test
    public void testSubWithScriptXml() throws InterruptedException {
        LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("hello s1", context.getData("test1"));
        Assertions.assertEquals("a==>b==>c==>s1[脚本s1]==>s2[脚本s2]", response.getExecuteStepStrWithoutTime());

        //添加和删除脚本
        addAndDeleteScriptData();
        //修改redis脚本
        changeScriptData();
        Thread.sleep(100);
        context = flowExecutor.execute2Resp("chain3", "arg").getFirstContextBean();
        Assertions.assertEquals("hello s1 version2", context.getData("test1"));
        context = flowExecutor.execute2Resp("chain2", "arg").getFirstContextBean();
        Assertions.assertEquals("hello s3 version2", context.getData("test2"));
    }

    /**
     * 修改redisson中的chain
     */
    public void changeXMLData() {
        RedisParserHelper.changeChain("chain1", "THEN(a, c, b);");
    }

    /**
     * 删除redisson中的chain
     */
    public void deleteXMLData() {
        FlowBus.removeChain("chain1");
        FlowBus.removeChain("chain4");
    }

    /**
     * 新增redisson中的chain
     */
    public void addXMLData() {
        RedisParserHelper.changeChain("chain4", "THEN(b, c);");
    }

    /**
     * 修改redisson中的脚本
     */
    public void changeScriptData() {
        RedisParserHelper.changeScriptNode("s1:script:脚本s1:groovy", "defaultContext.setData(\"test1\",\"hello s1 version2\");");
        RedisParserHelper.changeScriptNode("s3:script:脚本s3", "defaultContext.setData(\"test2\",\"hello s3 version2\");");
    }

    /**
     * 新增和删除redisson中的chain
     */
    public void addAndDeleteScriptData() {
        NodeConvertHelper.NodeSimpleVO nodeSimpleVO = NodeConvertHelper.convert("s3:script:脚本s3");
        FlowBus.unloadScriptNode(nodeSimpleVO.getNodeId());

        RedisParserHelper.changeScriptNode("s5:script:脚本s5:groovy",  "defaultContext.setData(\"test1\",\"hello s5\");");
    }


}
