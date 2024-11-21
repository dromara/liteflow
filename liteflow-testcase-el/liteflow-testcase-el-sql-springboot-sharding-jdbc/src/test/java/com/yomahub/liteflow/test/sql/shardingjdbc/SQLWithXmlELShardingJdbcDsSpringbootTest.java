package com.yomahub.liteflow.test.sql.shardingjdbc;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/application-sharding-jdbc-data-source-xml.properties")
@SpringBootTest(classes = SQLWithXmlELShardingJdbcDsSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.sql.shardingjdbc.cmp"})
public class SQLWithXmlELShardingJdbcDsSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testMultiLanguage1() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("x2[python脚本]==>x0[if 脚本]==>a==>b", response.getExecuteStepStrWithoutTime());
    }

}
