package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.enums.ParallelStrategyEnum;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.WhenCondition;

import java.util.HashSet;
import java.util.Set;

/**
 * EL 规则中的 must 的操作符
 *
 * @author luo yi
 * @since 2.11.0
 */
public class MustOperator extends BaseOperator<WhenCondition> {

	@Override
	public WhenCondition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeGteTwo(objects);

		String errorMsg = "The caller must be WhenCondition item";
		WhenCondition whenCondition = OperatorHelper.convert(objects[0], WhenCondition.class, errorMsg);

		// 解析指定完成的任务 ID 集合
		Set<String> specifyIdSet = new HashSet<>();

		for (int i = 1; i < objects.length; i++) {
			Object task = objects[i];
			if (task instanceof String) {
				specifyIdSet.add(OperatorHelper.convert(task, String.class));
			} else if (task instanceof Executable) {
				specifyIdSet.add(OperatorHelper.convert(task, Executable.class).getId());
			}
		}

		whenCondition.setSpecifyIdSet(specifyIdSet);
		whenCondition.setParallelStrategy(ParallelStrategyEnum.SPECIFY);
		return whenCondition;
	}

}
