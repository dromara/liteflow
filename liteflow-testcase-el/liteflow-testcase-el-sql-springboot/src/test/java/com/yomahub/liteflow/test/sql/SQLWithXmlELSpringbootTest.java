package com.yomahub.liteflow.test.sql;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
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
 * @author tangkc
 * @since 2.9.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/application-xml.properties")
@SpringBootTest(classes = SQLWithXmlELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.sql.cmp"})
public class SQLWithXmlELSpringbootTest extends BaseTest {
	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testSQLWithXml() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assert.assertEquals("a==>b==>c", response.getExecuteStepStr());
	}
}
