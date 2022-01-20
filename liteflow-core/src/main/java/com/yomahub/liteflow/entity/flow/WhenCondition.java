/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.entity.flow;

/**
 * 并行器
 * @author Bryan.Zhang
 */
public class WhenCondition extends Condition{

	public WhenCondition(Condition condition) {
		super(condition.getNodeList());
		super.setConditionType(condition.getConditionType());
		super.setGroup(condition.getGroup());
		super.setErrorResume(condition.isErrorResume());
		super.setAny(condition.isAny());
		super.setThreadExecutorClass(condition.getThreadExecutorClass());
	}

}
