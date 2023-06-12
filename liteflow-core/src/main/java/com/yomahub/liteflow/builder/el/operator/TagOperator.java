package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.collection.ListUtil;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.builder.el.operator.base.BaseOperator;
import com.yomahub.liteflow.builder.el.operator.base.OperatorHelper;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.element.condition.ThenCondition;

/**
 * EL规则中的tag的操作符
 *
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class TagOperator extends BaseOperator<Executable> {

	@Override
	public Executable build(Object[] objects) throws Exception {
		OperatorHelper.checkObjectSizeEqTwo(objects);

		Executable refObj = OperatorHelper.convert(objects[0], Executable.class);

		String tag = OperatorHelper.convert(objects[1], String.class);

		//如果解析的对象是一个Chain，由于Chain对象全局唯一，无法再进行复制
		//所以如果要给chain设置tag，则需要套上一个THEN，在THEN上面设置tag
		if (refObj instanceof Chain){
			ThenCondition wrapperChainCondition = new ThenCondition();
			wrapperChainCondition.setExecutableList(ListUtil.toList(refObj));
			wrapperChainCondition.setTag(tag);
			return wrapperChainCondition;
		}else{
			refObj.setTag(tag);
			return refObj;
		}
	}
}
