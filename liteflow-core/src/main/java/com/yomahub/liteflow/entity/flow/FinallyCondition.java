/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.entity.flow;

/**
 * 前置Condition
 * @author Bryan.Zhang
 * @since 2.6.4
 */
public class FinallyCondition extends Condition {

	public FinallyCondition(Condition condition){
		super(condition.getNodeList());
		super.setConditionType(condition.getConditionType());
	}
}
