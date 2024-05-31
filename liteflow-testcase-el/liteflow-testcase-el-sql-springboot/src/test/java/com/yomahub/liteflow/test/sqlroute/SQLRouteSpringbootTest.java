package com.yomahub.liteflow.test.sqlroute;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.parser.sql.exception.ELSQLException;
import com.yomahub.liteflow.parser.sql.vo.SQLParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.util.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author tangkc
 * @since 2.9.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/application-route.properties")
@SpringBootTest(classes = SQLRouteSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.sqlroute.cmp" })
public class SQLRouteSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testRoute1() {
		List<LiteflowResponse> responseList =  flowExecutor.executeRouteChain("ns", 15, DefaultContext.class);
		LiteflowResponse response1 = responseList.stream().filter(liteflowResponse -> liteflowResponse.getChainId().equals("r_chain1")).findFirst().orElse(null);
		Assertions.assertTrue(response1.isSuccess());
		Assertions.assertEquals("a==>b==>c", response1.getExecuteStepStr());
		LiteflowResponse response2 = responseList.stream().filter(liteflowResponse -> liteflowResponse.getChainId().equals("r_chain2")).findFirst().orElse(null);
		Assertions.assertTrue(response2.isSuccess());
		Assertions.assertEquals("c==>b==>a", response2.getExecuteStepStr());
	}



}
