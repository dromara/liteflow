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
import com.yomahub.liteflow.slot.DefaultContext;

@LiteflowComponent("h")
public class HCmp extends NodeComponent {

	@Override
	public void process() {
	}

	@Override
	public boolean isAccess() {
		DefaultContext context = this.getFirstContextBean();
		context.setData("test",this.getTag());
		return true;
	}
}
