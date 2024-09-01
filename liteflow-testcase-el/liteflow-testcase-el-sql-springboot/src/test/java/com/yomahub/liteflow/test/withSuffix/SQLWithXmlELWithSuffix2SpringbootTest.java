package com.yomahub.liteflow.test.withSuffix;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.parser.sql.read.CustomSqlRead;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

import static org.mockito.Mockito.when;

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

	@MockBean(name = "CustomSqlRead")
	private static CustomSqlRead customChain;

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testSQLWithXmlChain1() {
		LiteflowResponse response = flowExecutor.execute2Resp("r_chain4", "arg");
		Assertions.assertEquals("c==>b==>a", response.getExecuteStepStr());
	}


	@Test
	public void testSQLWithXmlChain2() {
		when(customChain.getCustomChainSql())
				.thenReturn(" SELECT APPLICATION_NAME,CHAIN_NAME,'THEN(c,b,c);' as EL_DATA,CUSTOM_FILTER_TYPE FROM EL_TABLE WHERE custom_filter_type = 'biz2' ");

	    flowExecutor.init(false);

		LiteflowResponse response = flowExecutor.execute2Resp("r_chain4", "arg");
		Assertions.assertEquals("c==>b==>c", response.getExecuteStepStr());
	}
}
