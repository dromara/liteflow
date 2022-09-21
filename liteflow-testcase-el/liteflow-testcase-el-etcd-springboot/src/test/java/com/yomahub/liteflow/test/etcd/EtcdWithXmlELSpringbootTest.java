package com.yomahub.liteflow.test.etcd;

import cn.hutool.core.util.ReflectUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.FlowParserTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.parser.etcd.EtcdClient;
import com.yomahub.liteflow.parser.etcd.EtcdXmlELParser;
import com.yomahub.liteflow.parser.etcd.util.EtcdParserHelper;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
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
        String flowXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\">THEN(a, b, c);</chain></flow>";
        when(etcdClient.get(anyString())).thenReturn(flowXml);

        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b==>c", response.getExecuteStepStr());
    }

    @Test
    public void testEtcdNodeWithXml2() throws Exception {
        String flowXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\">THEN(a, b, c);</chain></flow>";
        String changedFlowXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\">THEN(a, c);</chain></flow>";
        when(etcdClient.get(anyString())).thenReturn(flowXml).thenReturn(changedFlowXml);

        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b==>c", response.getExecuteStepStr());

        // 手动触发一次 模拟节点数据变更
        FlowBus.refreshFlowMetaData(FlowParserTypeEnum.TYPE_EL_XML,changedFlowXml);

        LiteflowResponse response2 = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response2.isSuccess());
        Assert.assertEquals("a==>c", response2.getExecuteStepStr());
    }
}
