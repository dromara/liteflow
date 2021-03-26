/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.entity.flow;

import java.util.List;

/**
 * 并行器
 * @author Bryan.Zhang
 */
public class WhenCondition extends Condition{
	//	增加errorResume属性，以区分当when调用链调用失败时是否继续往下执行
	private boolean errorResume;

	public WhenCondition(List<Executable> nodeList) {
		super(nodeList);
		errorResume = true;
	}

	public WhenCondition(List<Executable> nodeList, boolean errorResume) {
		super(nodeList);
		this.errorResume = errorResume;
	}

	public boolean isErrorResume() {
		return errorResume;
	}
}
