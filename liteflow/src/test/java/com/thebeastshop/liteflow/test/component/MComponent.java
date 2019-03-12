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
import com.thebeastshop.liteflow.core.NodeCondComponent;

@Component("m")
public class MComponent extends NodeCondComponent {

	@Resource
	private FlowExecutor flowExecutor;
	
	@Override
	protected Class<? extends NodeComponent> processCond() throws Exception {
		System.out.println("m conponent executed");
		Integer flag = this.getSlot().getChainReqData("strategy1");
		if(flag == 1) {
			return M1Component.class;
		}else if(flag == 2){
			return M2Component.class;
		}else {
			return M3Component.class;
		}
		
	}
	
}
