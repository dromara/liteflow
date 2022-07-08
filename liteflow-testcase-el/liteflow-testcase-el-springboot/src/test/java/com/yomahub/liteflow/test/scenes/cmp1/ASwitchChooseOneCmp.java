/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.scenes.cmp1;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeSwitchComponent;
import com.yomahub.liteflow.test.scenes.CmpUtil;
import com.yomahub.liteflow.test.scenes.bean.MarketingDO;
import com.yomahub.liteflow.test.scenes.bean.RequestDataDO;

import java.util.HashMap;
import java.util.Map;

/**
 * springboot环境EL复杂例子测试1
 * 场景描述:可以参与的营销 ASwitchCmp
 * 1条件:开始时间+区域.
 * 2流程:根据营销信息(类型+模式)->筛选可参与营销(类型+模式)
 * 3触发:THEN
 * @author nmnl
 */
// @LiteflowComponent("ASwitchCmp")
// @Component("ASwitchCmp")
@LiteflowComponent("a")
public class ASwitchChooseOneCmp extends NodeSwitchComponent {

	// 简单数据结构
	// 虚拟:营销数据
	private final static Map<String, MarketingDO> CHOOSE_ONE = new HashMap<>(3);

	static {
		MarketingDO a = new MarketingDO();
		CHOOSE_ONE.put("a",a);

		MarketingDO b = new MarketingDO();
		CHOOSE_ONE.put("b",b);

		MarketingDO c = new MarketingDO();
		CHOOSE_ONE.put("c",c);
	}

	@Override
	public String processSwitch() throws Exception {
		RequestDataDO requestDataDO = this.getRequestData();
		if (null == requestDataDO){
			// requestDataDO 为空,无法做进一步的判断
			throw new Exception("requestDataDO 为空");
		}
		// 1条件:营销开始时间+区域..
		MarketingDO chooseOne = CHOOSE_ONE.get(requestDataDO.getInstId());
		if (null == chooseOne){
			// 未命中活动
			throw new Exception("未命中营销");
		}
		//this.getSlot().setResponseData(chooseOne);
		return CmpUtil.processSwitch(chooseOne.getMode(),chooseOne.getType());

	}
}
