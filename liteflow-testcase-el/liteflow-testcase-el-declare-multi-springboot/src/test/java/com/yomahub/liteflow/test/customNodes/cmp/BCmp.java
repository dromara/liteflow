/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.customNodes.cmp;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.test.customNodes.domain.DemoDomain;

import javax.annotation.Resource;

public class BCmp {

	@Resource
	private DemoDomain demoDomain;

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void process(NodeComponent bindCmp) {
		demoDomain.sayHi();
		System.out.println("BCmp executed!");
	}

}
