package com.yomahub.liteflow.test.nacos;

import com.alibaba.nacos.client.config.impl.LocalConfigInfoProcessor;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.FlowParserTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.parser.nacos.util.NacosParserHelper;
import com.yomahub.liteflow.parser.nacos.vo.NacosParserVO;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * springboot环境下的nacos配置源功能测试
 * nacos存储数据的格式为xml文件
 * @author mll
 * @since 2.9.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/nacos/application-xml.properties")
@SpringBootTest(classes = NacosWithXmlELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.nacos.cmp"})
public class NacosWithXmlELSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    private NacosParserHelper helper;

    // 写到本地.
    final String flowXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\">THEN(a, b, c);</chain></flow>";

    // 2次写入本地
    final String changedFlowXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\">THEN(a, c);</chain></flow>";

    private static NacosParserVO nacosParserVO = new NacosParserVO();

    static {
        // 这里的信息必须 = application-xml.properties liteflow.rule-source-ext-data
        nacosParserVO.setServerAddr("127.0.0.1:8848");
        nacosParserVO.setDataId("LiteFlow");
        nacosParserVO.setGroup("LITE_FLOW_GROUP");
        nacosParserVO.setNamespace("");
    }

    @After
    public void clean() {
        LocalConfigInfoProcessor.cleanAllSnapshot();
    }

    @Test
    public void testNacosWithXml() throws Exception {
        // envName 把数据写到本地,作为快照数据.始终会取到
        String envName = "fixed-127.0.0.1_8848";
        LocalConfigInfoProcessor.saveSnapshot(envName,nacosParserVO.getDataId(),nacosParserVO.getGroup(),nacosParserVO.getNamespace(),flowXml);
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertEquals("a==>b==>c", response.getExecuteStepStrWithoutTime());

        FlowBus.refreshFlowMetaData(FlowParserTypeEnum.TYPE_EL_XML,changedFlowXml);
        response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertEquals("a==>c", response.getExecuteStepStrWithoutTime());
    }
}
