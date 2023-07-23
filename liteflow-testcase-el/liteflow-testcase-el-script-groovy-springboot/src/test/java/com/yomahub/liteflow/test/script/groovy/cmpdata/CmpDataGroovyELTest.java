package com.yomahub.liteflow.test.script.groovy.cmpdata;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * springboot环境EL常规的例子测试
 *
 * @author Bryan.Zhang
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/cmpdata/application.properties")
@SpringBootTest(classes = CmpDataGroovyELTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.groovy.cmpdata.cmp" })
public class CmpDataGroovyELTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 最简单的情况
	@Test
	public void testCmpData1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("1995-10-01", context.getData("s1"));
	}

}
