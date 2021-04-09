/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.executor.cmp;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.test.executor.CustomSlot;
import org.springframework.stereotype.Component;

@Component("c")
public class CCmp extends NodeComponent {

	@Override
	public void process() {
		if(this.getSlot() instanceof CustomSlot) {
			CustomSlot slot = this.getSlot();
			String str = slot.getRequestData();
			if(StrUtil.isNotBlank(str) && str.equals("test0")) {
				slot.setName("custom");
			}
			if(StrUtil.isNotBlank(str) && str.equals("test1")) {
				slot.setName("custom");
				throw new RuntimeException("customException");
			}
		}
		System.out.println("CCmp executed!");
	}

}
