package com.yomahub.liteflow.flow.element.condition;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;

/**
 * 循环条件Condition
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public class WhileCondition extends LoopCondition {

	@Override
	public void executeCondition(Integer slotIndex) throws Exception {
		Executable whileItem = this.getWhileItem();

		// 先去判断isAccess方法，如果isAccess方法都返回false，整个WHILE表达式不执行
		if (!whileItem.isAccess(slotIndex)) {
			return;
		}

		// 获得要循环的可执行对象
		Executable executableItem = this.getDoExecutor();

		// 获取Break节点
		Executable breakItem = this.getBreakItem();

		// 循环执行
		int index = 0;
		while (getWhileResult(slotIndex)) {
			executableItem.setCurrChainId(this.getCurrChainId());
			setLoopIndex(executableItem, index);
			executableItem.execute(slotIndex);
			// 如果break组件不为空，则去执行
			if (ObjectUtil.isNotNull(breakItem)) {
				breakItem.setCurrChainId(this.getCurrChainId());
				setLoopIndex(breakItem, index);
				breakItem.execute(slotIndex);
				boolean isBreak = breakItem.getItemResultMetaValue(slotIndex);
				if (isBreak) {
					break;
				}
			}
			index++;
		}
	}

	private boolean getWhileResult(Integer slotIndex) throws Exception {
		Executable whileItem = this.getWhileItem();
		// 执行while组件
		whileItem.setCurrChainId(this.getCurrChainId());
		whileItem.execute(slotIndex);

		return whileItem.getItemResultMetaValue(slotIndex);
	}

	@Override
	public ConditionTypeEnum getConditionType() {
		return ConditionTypeEnum.TYPE_WHILE;
	}

	public Executable getWhileItem() {
		return this.getExecutableOne(ConditionKey.WHILE_KEY);
	}

	public void setWhileItem(Executable whileItem) {
		this.addExecutable(ConditionKey.WHILE_KEY, whileItem);
	}

}
