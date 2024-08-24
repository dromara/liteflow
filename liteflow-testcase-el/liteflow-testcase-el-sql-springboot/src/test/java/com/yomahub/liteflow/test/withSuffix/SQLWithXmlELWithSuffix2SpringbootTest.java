package com.yomahub.liteflow.test.withSuffix;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
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
import javax.sql.DataSource;

/**
 * @author jay li
 * @since 2.12.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/application-data-source-with-suffix2-xml.properties")
@SpringBootTest(classes = SQLWithXmlELWithSuffix2SpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.sql.cmp"})
public class SQLWithXmlELWithSuffix2SpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testSQLWithXmlChain1() {
		LiteflowResponse response = flowExecutor.execute2Resp("r_chain1", "arg");
		Assertions.assertEquals("c==>b==>a", response.getExecuteStepStr());
	}

}
