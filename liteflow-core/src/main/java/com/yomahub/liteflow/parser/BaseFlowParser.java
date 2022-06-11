package com.yomahub.liteflow.parser;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.LiteFlowChainBuilder;
import com.yomahub.liteflow.builder.LiteFlowConditionBuilder;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.prop.ChainPropBean;
import com.yomahub.liteflow.builder.prop.NodePropBean;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.EmptyConditionValueException;
import com.yomahub.liteflow.exception.NodeTypeNotSupportException;
import com.yomahub.liteflow.exception.NotSupportConditionException;

/**
 * 基类，用于存放通用方法
 *
 * @author tangkc
 */
public abstract class BaseFlowParser implements FlowParser {

	/**
	 * 构建 node
	 *
	 * @param nodePropBean 构建 node 的中间属性
	 */
	public void buildNode(NodePropBean nodePropBean) {
		String id = nodePropBean.getId();
		String name = nodePropBean.getName();
		String clazz = nodePropBean.getClazz();
		String script = nodePropBean.getScript();
		String type = nodePropBean.getType();
		String file = nodePropBean.getFile();

		//初始化type
		if (StrUtil.isBlank(type)) {
			type = NodeTypeEnum.COMMON.getCode();
		}

		//检查nodeType是不是规定的类型
		NodeTypeEnum nodeTypeEnum = NodeTypeEnum.getEnumByCode(type);
		if (ObjectUtil.isNull(nodeTypeEnum)) {
			throw new NodeTypeNotSupportException(StrUtil.format("type [{}] is not support", type));
		}

		//进行node的build过程
		LiteFlowNodeBuilder.createNode().setId(id).setName(name).setClazz(clazz).setType(nodeTypeEnum).setScript(script).setFile(file).build();
	}


	/**
	 * 构建 chain
	 *
	 * @param chainPropBean 构建 chain 的中间属性
	 * @param chainBuilder  chainBuilder
	 */
	public void buildChain(ChainPropBean chainPropBean
			, LiteFlowChainBuilder chainBuilder) {
		String condValueStr = chainPropBean.getCondValueStr();
		String group = chainPropBean.getGroup();
		String errorResume = chainPropBean.getErrorResume();
		String any = chainPropBean.getAny();
		String threadExecutorClass = chainPropBean.getThreadExecutorClass();
		ConditionTypeEnum conditionType = chainPropBean.getConditionType();

		if (ObjectUtil.isNull(conditionType)) {
			throw new NotSupportConditionException("ConditionType is not supported");
		}

		if (StrUtil.isBlank(condValueStr)) {
			throw new EmptyConditionValueException("Condition value cannot be empty");
		}

		//如果是when类型的话，有特殊化参数要设置，只针对于when的
		if (conditionType.equals(ConditionTypeEnum.TYPE_WHEN)) {
			chainBuilder.setCondition(
					LiteFlowConditionBuilder.createWhenCondition()
							.setErrorResume(errorResume)
							.setGroup(group)
							.setAny(any)
							.setThreadExecutorClass(threadExecutorClass)
							.setValue(condValueStr)
							.build()
			).build();
		} else {
			chainBuilder.setCondition(
					LiteFlowConditionBuilder.createCondition(conditionType)
							.setValue(condValueStr)
							.build()
			).build();
		}
	}
}
