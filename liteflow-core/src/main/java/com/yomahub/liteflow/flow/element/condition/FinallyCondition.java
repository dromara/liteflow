/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.flow.element.condition;

import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.flow.element.Executable;

/**
 * 前置Condition
 * @author Bryan.Zhang
 * @since 2.6.4
 */
public class FinallyCondition extends Condition {

	@Override
	public void execute(Integer slotIndex) throws Exception {
		for(Executable executableItem : this.getNodeList()){
			executableItem.execute(slotIndex);
		}
	}

	@Override
	public ConditionTypeEnum getConditionType() {
		return ConditionTypeEnum.TYPE_FINALLY;
	}
}
