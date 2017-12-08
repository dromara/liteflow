package com.thebeastshop.liteflow.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		
		for(int i=0;i<10;i++){
			executorService.submit(new Thread(){
				@Override
				public void run() {
					String response = flowExecutor.execute("chain1", "it's a request");
					System.out.println(response);
				}
			});
		}
		System.out.println("done!");
		System.in.read();
	}
	
	@Test
	public void test2() throws Exception {
		try {
			String response = flowExecutor.execute("chain3", "it's a request");
			System.out.println(response);
			System.in.read();
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
}
