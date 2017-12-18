/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-8-1
 * @version 1.0
 */
package com.thebeastshop.liteflow.test.component;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.thebeastshop.liteflow.core.FlowExecutor;
import com.thebeastshop.liteflow.core.NodeComponent;

@Component("p2")
public class P2Component extends NodeComponent {

	@Resource
	private FlowExecutor flowExecutor;
	
	@Override
	public void process() {
		System.out.println("p2 component executed!");
	}
	
}
