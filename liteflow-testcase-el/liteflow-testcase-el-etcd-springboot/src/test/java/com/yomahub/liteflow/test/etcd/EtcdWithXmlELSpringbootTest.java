package com.yomahub.liteflow.test.etcd;

import com.google.common.collect.Lists;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.parser.etcd.EtcdClient;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;
import static org.mockito.Mockito.*;

/**
 * springboot环境下的etcd 规则解析器 测试
 */
@RunWith(SpringRunner.class)
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

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void after(){
        FlowBus.cleanCache();
    }

    @Test
    public void testEtcdNodeWithXml1() throws Exception {
        List<String> chainNameList = Lists.newArrayList("chain1");
        List<String> scriptNodeValueList = Lists.newArrayList("s1:script:脚本s1");
        when(etcdClient.getChildrenKeys(anyString(), anyString())).thenReturn(chainNameList).thenReturn(scriptNodeValueList);

        String chain1Data = "THEN(a, b, c, s1);";
        String scriptNodeValue = "defaultContext.setData(\"test\",\"hello\");";
        when(etcdClient.get(anyString())).thenReturn(chain1Data).thenReturn(scriptNodeValue);

        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b==>c==>s1[脚本s1]", response.getExecuteStepStr());
        Assert.assertEquals("hello", context.getData("test"));
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
        when(etcdClient.get(SCRIPT_PATH + SEPARATOR + "s1:script:脚本s1")).thenReturn(scriptNodeValue).thenReturn(scriptNodeChangedValue);

        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b==>c==>s1[脚本s1]", response.getExecuteStepStr());
        Assert.assertEquals("hello", context.getData("test"));

        flowExecutor.reloadRule();

        LiteflowResponse response2 = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context2 = response2.getFirstContextBean();
        Assert.assertTrue(response2.isSuccess());
        Assert.assertEquals("a==>b==>s1[脚本s1]", response2.getExecuteStepStr());
        Assert.assertEquals("hello world", context2.getData("test"));

    }
}
