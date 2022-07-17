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

@LiteflowComponent("f")
public class FCmp extends NodeComponent {

	@Override
	public void process() {
		System.out.println("FCmp executed!");
	}

	@Override
	public boolean isAccess() {
		return Boolean.parseBoolean(this.getTag());
	}
}
