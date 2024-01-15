package com.yomahub.liteflow.builder.el.operator;

import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.element.condition.CatchCondition;
import com.yomahub.liteflow.flow.element.condition.ThenCondition;

/**
 * EL规则中的CATCH的操作符 用法：CATCH...DO...
 *
 * @author Bryan.Zhang
 * @since 2.10.0
 */
public class CatchOperator extends BaseOperator<CatchCondition> {

	@Override
	public CatchCondition build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEq(objects, 1);

		OperatorHelper.checkObjMustBeCommonTypeItem(objects[0]);

		Executable catchItem = OperatorHelper.convert(objects[0], Executable.class);

		CatchCondition catchCondition = new CatchCondition();

		//如果是单个Node的话，要包装成THEN的CONDITION模式，否则CATCH不到异常
		if (catchItem instanceof Node){
			ThenCondition thenCondition = new ThenCondition();
			thenCondition.addExecutable(catchItem);
			catchCondition.setCatchItem(thenCondition);
		}else{
			catchCondition.setCatchItem(catchItem);
		}
		return catchCondition;
	}

}
