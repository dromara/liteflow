/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.script.javaxpro.override.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

@LiteflowComponent("b")
public class BCmp extends NodeComponent {

	@Override
	public void process() {
		throw new RuntimeException("error");
	}

	@Override
	public void rollback() throws Exception {
		super.rollback();
	}
}
