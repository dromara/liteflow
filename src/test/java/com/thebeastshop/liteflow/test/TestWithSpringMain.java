package com.thebeastshop.liteflow.test;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.thebeastshop.liteflow.core.FlowExecutor;



@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring-test.xml" })
public class TestWithSpringMain {
	
	@Resource
	private FlowExecutor flowExecutor;
	
	@Test
	public void test1() throws Exception {
		String response = flowExecutor.execute("chain2", "it's a request");
		System.out.println(response);
	}
}
