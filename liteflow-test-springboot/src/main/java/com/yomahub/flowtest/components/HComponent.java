/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.flowtest.components;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("h")
public class HComponent extends NodeComponent {

	@Resource
	private FlowExecutor flowExecutor;

	@Override
	public void process() {
		System.out.println("Hcomponent executed!");
		try{
			flowExecutor.invoke("strategy1",3, DefaultSlot.class, this.getSlotIndex());
		}catch (Exception e){
			e.printStackTrace();
		}
	}
}
