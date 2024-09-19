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
@TestPropertySource(value = "classpath:/application-data-source-with-suffix-xml.properties")
@SpringBootTest(classes = SQLWithXmlELWithSuffixSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.sql.cmp"})
public class SQLWithXmlELWithSuffixSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testSQLWithXmlChain1() {
		LiteflowResponse response = flowExecutor.execute2Resp("r_chain3", "arg");
		Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());
	}
}
