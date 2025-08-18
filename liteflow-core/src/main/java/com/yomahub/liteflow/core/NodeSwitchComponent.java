/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.core;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.condition.SwitchCondition;
import com.yomahub.liteflow.slot.DataBus;
import com.yomahub.liteflow.core.proxy.LiteFlowProxyUtil;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 条件路由节点抽象类
 *
 * @author Bryan.Zhang
 */
public abstract class NodeSwitchComponent extends NodeComponent {

	@Override
	public void process() throws Exception {
		String nodeId = this.processSwitch();
		this.getSlot().setSwitchResult(this.getMetaValueKey(), nodeId);
	}

	// 用以返回路由节点的beanId
	public abstract String processSwitch() throws Exception;

	@Override
	@SuppressWarnings("unchecked")
	public String getItemResultMetaValue(Integer slotIndex) {
		return DataBus.getSlot(slotIndex).getSwitchResult(this.getMetaValueKey());
	}

	public List<String> getTargetList(){
		Condition condition = this.getSlot().getCurrentCondition();
		if (condition instanceof SwitchCondition){
			return ((SwitchCondition)condition).getTargetList().stream().map(Executable::getId).collect(Collectors.toList());
		}else{
			return ListUtil.empty();
		}
	}

}
