package com.yomahub.liteflow.test.etcd;

import com.google.common.collect.Lists;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ChainNotFoundException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.parser.etcd.EtcdClient;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * springboot环境下的etcd 规则解析器 测试
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/etcd/application-xml-cluster.properties")
@SpringBootTest(classes = EtcdWithXmlELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.etcd.cmp"})
public class EtcdWithXmlELSpringbootTest extends BaseTest {

    @MockBean
    private EtcdClient etcdClient;

    @Resource
    private FlowExecutor flowExecutor;

    private static final String SEPARATOR = "/";

    private static final String CHAIN_PATH = "/liteflow/chain";

    private static final String SCRIPT_PATH = "/liteflow/script";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @AfterEach
    public void after() {
        FlowBus.cleanCache();
        FlowBus.clearStat();
    }

    @Test
    public void testEtcdNodeWithXml1() throws Exception {
        List<String> chainNameList = Lists.newArrayList("chain1", "chain2:false");
        List<String> scriptNodeValueList = Lists.newArrayList("s1:script:脚本s1", "s2:script:脚本s1:groovy:false");
        when(etcdClient.getChildrenKeys(CHAIN_PATH, SEPARATOR)).thenReturn(chainNameList);
        when(etcdClient.getChildrenKeys(SCRIPT_PATH, SEPARATOR)).thenReturn(scriptNodeValueList);

        when(etcdClient.get(CHAIN_PATH + "/chain1")).thenReturn("THEN(a, b, c, s1);");
        when(etcdClient.get(CHAIN_PATH + "/chain2:false")).thenReturn("THEN(a, b, c, s1);");
        when(etcdClient.get(SCRIPT_PATH + "/s1:script:脚本s1")).thenReturn("defaultContext.setData(\"test\",\"hello\");");
        when(etcdClient.get(SCRIPT_PATH + "/s2:script:脚本s1:groovy:false")).thenReturn("defaultContext.setData(\"test\",\"hello\");");

        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>b==>c==>s1[脚本s1]", response.getExecuteStepStr());
        Assertions.assertEquals("hello", context.getData("test"));

        // 测试 chain 停用
        Assertions.assertThrows(ChainNotFoundException.class, () -> {
            throw flowExecutor.execute2Resp("chain2", "arg").getCause();
        });

        // 测试 script 停用
        Assertions.assertTrue(!FlowBus.getNodeMap().containsKey("s2"));
    }

    @Test
    public void testEtcdNodeWithXml2() throws Exception {
        List<String> chainNameList = Lists.newArrayList("chain1");
        List<String> scriptNodeValueList = Lists.newArrayList("s1:script:脚本s1");
        when(etcdClient.getChildrenKeys(CHAIN_PATH, SEPARATOR)).thenReturn(chainNameList);
        when(etcdClient.getChildrenKeys(SCRIPT_PATH, SEPARATOR)).thenReturn(scriptNodeValueList);

        String chain1Data = "THEN(a, b, c, s1);";
        String chain1ChangedData = "THEN(a, b, s1);";
        String scriptNodeValue = "defaultContext.setData(\"test\",\"hello\");";
        String scriptNodeChangedValue = "defaultContext.setData(\"test\",\"hello world\");";
        when(etcdClient.get(CHAIN_PATH + SEPARATOR + "chain1")).thenReturn(chain1Data).thenReturn(chain1ChangedData);
        when(etcdClient.get(SCRIPT_PATH + SEPARATOR + "s1:script:脚本s1")).thenReturn(scriptNodeValue)
                .thenReturn(scriptNodeChangedValue);

        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>b==>c==>s1[脚本s1]", response.getExecuteStepStr());
        Assertions.assertEquals("hello", context.getData("test"));

        flowExecutor.reloadRule();

        LiteflowResponse response2 = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context2 = response2.getFirstContextBean();
        Assertions.assertTrue(response2.isSuccess());
        Assertions.assertEquals("a==>b==>s1[脚本s1]", response2.getExecuteStepStr());
        Assertions.assertEquals("hello world", context2.getData("test"));

    }

}
