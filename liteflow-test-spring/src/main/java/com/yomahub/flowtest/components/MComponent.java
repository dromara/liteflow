/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.flowtest.components;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.NodeCondComponent;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("m")
public class MComponent extends NodeCondComponent {

	@Resource
	private FlowExecutor flowExecutor;

	@Override
	public String processCond() throws Exception {
		System.out.println("m conponent executed");
		Integer flag = this.getSlot().getData("m_flag");
		if(flag == 1) {
			return "m1";
		}else if(flag == 2){
			return "m2";
		}else if(flag == 3){
			return "m3";
		}else{
			return "strategy2";
		}

	}

}
