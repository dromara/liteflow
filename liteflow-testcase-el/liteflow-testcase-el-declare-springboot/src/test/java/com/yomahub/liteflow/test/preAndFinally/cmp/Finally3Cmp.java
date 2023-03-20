/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.preAndFinally.cmp;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import org.springframework.stereotype.Component;

@Component("f3")
public class Finally3Cmp {

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void process(NodeComponent bindCmp) throws Exception {
		Slot slot = bindCmp.getSlot();
		DefaultContext context = slot.getFirstContextBean();
		if (ObjectUtil.isNull(slot.getException())) {
			context.setData("hasEx", false);
		}
		else {
			context.setData("hasEx", true);
		}
		System.out.println("Finally3Cmp executed!");
	}

}
