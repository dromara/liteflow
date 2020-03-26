/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-8-1
 * @version 1.0
 */
package com.yomahub.flowtest.components;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("m3")
public class M3Component extends NodeComponent {

	@Resource
	private FlowExecutor flowExecutor;

	@Override
	public void process() {
		System.out.println("m3 component executed!");
		try{
			flowExecutor.invoke("strategy2",10, DefaultSlot.class, this.getSlotIndex());
		}catch (Exception e){
			e.printStackTrace();
		}
	}
}
