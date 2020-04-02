/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.core;

import org.springframework.stereotype.Component;

public abstract class NodeCondComponent extends NodeComponent {

	@Override
	public void process() throws Exception {
		String nodeId = this.processCond();
		this.getSlot().setCondResult(this.getClass().getName(), nodeId);
	}

	public abstract String processCond() throws Exception;

}
