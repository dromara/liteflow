package com.yomahub.liteflow.test.whenTimeOut;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.WhenTimeoutException;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * springboot环境下异步线程超时日志打印测试
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/whenTimeOut/application1.properties")
@SpringBootTest(classes = WhenTimeOutELDeclMultiSpringbootTest1.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.whenTimeOut.cmp" })
public class WhenTimeOutELDeclMultiSpringbootTest1 extends BaseTest {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Resource
	private FlowExecutor flowExecutor;

	// 其中b和c在when情况下超时，所以抛出了WhenTimeoutException这个错
	@Test
	public void testWhenTimeOut() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals(WhenTimeoutException.class, response.getCause().getClass());
	}

}
