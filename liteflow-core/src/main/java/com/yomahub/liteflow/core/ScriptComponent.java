package com.yomahub.liteflow.core;

import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.script.ScriptExecuteWrap;

import java.util.HashMap;
import java.util.Map;

/**
 * 脚本接口
 *
 * @author Bryan.Zhang
 * @since 2.9.0
 */
public interface ScriptComponent {

	/**
	 * 用于维护脚本类型和脚本 cmp 的映射关系
	 */
	Map<NodeTypeEnum, Class<?>> ScriptComponentClassMap = new HashMap<NodeTypeEnum, Class<?>>() {
		{
			put(NodeTypeEnum.SCRIPT, ScriptCommonComponent.class);
			put(NodeTypeEnum.SWITCH_SCRIPT, ScriptSwitchComponent.class);
			put(NodeTypeEnum.BOOLEAN_SCRIPT, ScriptBooleanComponent.class);
			put(NodeTypeEnum.FOR_SCRIPT, ScriptForComponent.class);
		}
	};

	/**
	 * 加载脚本
	 * @param script
	 */
	void loadScript(String script, String language);

	default ScriptExecuteWrap buildWrap(NodeComponent cmp){
		ScriptExecuteWrap wrap = new ScriptExecuteWrap();
		wrap.setCurrChainId(cmp.getCurrChainId());
		wrap.setNodeId(cmp.getNodeId());
		wrap.setSlotIndex(cmp.getSlotIndex());
		wrap.setTag(cmp.getTag());
		wrap.setCmpData(cmp.getCmpData(Map.class));
		wrap.setLoopIndex(cmp.getLoopIndex());
		wrap.setLoopObject(cmp.getCurrLoopObj());
		wrap.setCmp(cmp);
		return wrap;
	}

}
