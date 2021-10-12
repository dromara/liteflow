/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.tag.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.data.Slot;

import java.util.HashSet;

@LiteflowComponent("a")
public class ACmp extends NodeComponent {

	@Override
	public void process() {
		String testKey = "test";

		Slot slot = this.getSlot();
		if (slot.getData(testKey) == null){
			slot.setData(testKey,this.getTag());
		}else{
			String s = slot.getData(testKey);
			s += this.getTag();
			slot.setData(testKey, s);
		}
	}
}
