/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.event.cmp;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

@Component("d")
public class DCmp {

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void process(NodeComponent bindCmp) throws Exception {
		System.out.println("CCmp executed!");
		throw new NullPointerException();
	}

	@LiteflowMethod(LiteFlowMethodEnum.ON_ERROR)
	public void onError(NodeComponent bindCmp, Exception e) throws Exception {
		DefaultContext context = bindCmp.getFirstContextBean();
		context.setData("error", "error:" + bindCmp.getNodeId());
	}

}
