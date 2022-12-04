package com.yomahub.liteflow.test.apollo;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
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
 * @Description:
 * @Author: zhanghua
 * @Date: 2022/12/3 15:22
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/apollo/application-xml.properties")
@SpringBootTest(classes = ApolloWithXmlELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.apollo.cmp"})
public class ApolloWithXmlELSpringbootTest {


	@Resource
	private FlowExecutor flowExecutor;

	@After
	public void after() {
		FlowBus.cleanCache();
	}


	@Test
	public void testApolloWithXml1() throws InterruptedException {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assert.assertEquals("a==>b==>c==>s1[脚本s1]", response.getExecuteStepStrWithoutTime());
	}


	@Test
	public void testApolloWithXml2() throws InterruptedException {
		while (true) {
			LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
			System.out.println("liteflow step : " + response.getExecuteStepStrWithoutTime());
			Thread.sleep(2000l);
		}
	}

}
