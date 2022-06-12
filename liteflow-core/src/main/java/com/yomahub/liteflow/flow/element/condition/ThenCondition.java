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
 * 串行器
 * @author Bryan.Zhang
 */
public class ThenCondition extends Condition {
	@Override
	public ConditionTypeEnum getConditionType() {
		return ConditionTypeEnum.TYPE_THEN;
	}

	@Override
	public void execute(Integer slotIndex) throws Exception {
		for (Executable executableItem : this.getNodeList()) {
			executableItem.execute(slotIndex);
		}
	}
}
