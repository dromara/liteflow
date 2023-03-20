/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.customMethodName.cmp;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.Slot;
import org.springframework.stereotype.Component;

@Component("a")
public class ACmp {

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void processAcmp(NodeComponent bindCmp) {
		System.out.println("ACmp executed!");
	}

	@LiteflowMethod(LiteFlowMethodEnum.IS_ACCESS)
	public boolean isAcmpAccess(NodeComponent bindCmp) {
		return true;
	}

	@LiteflowMethod(LiteFlowMethodEnum.BEFORE_PROCESS)
	public void beforeAcmp(NodeComponent bindCmp) {
		System.out.println("before A");
	}

	@LiteflowMethod(LiteFlowMethodEnum.AFTER_PROCESS)
	public void afterAcmp(NodeComponent bindCmp) {
		System.out.println("after A");
	}

	@LiteflowMethod(LiteFlowMethodEnum.ON_SUCCESS)
	public void onAcmpSuccess(NodeComponent bindCmp) {
		System.out.println("Acmp success");
	}

	@LiteflowMethod(LiteFlowMethodEnum.ON_ERROR)
	public void onAcmpError(NodeComponent bindCmp) {
		System.out.println("Acmp error");
	}

	@LiteflowMethod(LiteFlowMethodEnum.IS_END)
	public boolean isAcmpEnd(NodeComponent bindCmp) {
		System.out.println("Acmp end config");
		return false;
	}

}
