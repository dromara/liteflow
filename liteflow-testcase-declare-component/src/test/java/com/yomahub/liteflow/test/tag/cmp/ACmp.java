/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.tag.cmp;

import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;

@LiteflowComponent("a")
@LiteflowCmpDefine
public class ACmp{

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void process(NodeComponent bindCmp) {
		String testKey = "test";

		Slot slot = bindCmp.getSlot();
		if (slot.getData(testKey) == null){
			slot.setData(testKey,bindCmp.getTag());
		}else{
			String s = slot.getData(testKey);
			s += bindCmp.getTag();
			slot.setData(testKey, s);
		}
	}
}
