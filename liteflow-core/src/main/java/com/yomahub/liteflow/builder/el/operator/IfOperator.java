package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.collection.ListUtil;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.element.condition.AndOrCondition;
import com.yomahub.liteflow.flow.element.condition.IfCondition;
import com.yomahub.liteflow.flow.element.condition.NotCondition;

/**
 * EL规则中的IF的操作符
 *
 * @author Bryan.Zhang
 * @since 2.8.5
 */
public class IfOperator extends BaseOperator<IfCondition> {

	@Override
	public IfCondition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEq(objects, 2, 3);

		// 解析第一个参数
		Executable ifItem = OperatorHelper.convert(objects[0], Executable.class);
		OperatorHelper.checkObjectMustBeBooleanItem(ifItem);

		// 解析第二个参数
		Executable trueCaseExecutableItem = OperatorHelper.convert(objects[1], Executable.class);

		// 解析第三个参数，如果有的话
		Executable falseCaseExecutableItem = null;
		if (objects.length == 3) {
			falseCaseExecutableItem = OperatorHelper.convert(objects[2], Executable.class);
		}

		IfCondition ifCondition = new IfCondition();
		ifCondition.setIfItem(ifItem);
		ifCondition.setTrueCaseExecutableItem(trueCaseExecutableItem);
		ifCondition.setFalseCaseExecutableItem(falseCaseExecutableItem);
		return ifCondition;
	}

}
