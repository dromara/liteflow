/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-7-27
 * @version 1.0
 */
package com.thebeastshop.liteflow.test;

import java.util.Arrays;

import com.thebeastshop.liteflow.core.FlowExecutor;
import com.thebeastshop.liteflow.parser.FlowParser;

public class TestMain {
	public static void main(String[] args) throws Exception {
		final FlowExecutor executor = new FlowExecutor();
		executor.setRulePath(Arrays.asList(new String[]{"flow.xml"}));
		executor.init();
		
		for(int i=0;i<1;i++){
			String response = executor.execute("chain2", "it's a request");
			System.out.println(response);
		}
		
		
	}
	
}
