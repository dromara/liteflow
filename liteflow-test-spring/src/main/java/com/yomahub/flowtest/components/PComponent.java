/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.flowtest.components;

import com.yomahub.liteflow.core.NodeCondComponent;
import org.springframework.stereotype.Component;

@Component("p")
public class PComponent extends NodeCondComponent {

	@Override
	public String processCond() throws Exception {
		System.out.println("p conponent executed");
		Integer flag = this.getSlot().getData("p_flag");
		if(flag == 10) {
			return "p1";
		}else {
			return "p2";
		}

	}

}
