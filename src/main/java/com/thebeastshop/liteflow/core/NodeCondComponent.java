/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-7-28
 * @version 1.0
 */
package com.thebeastshop.liteflow.core;

public abstract class NodeCondComponent extends NodeComponent {

	@Override
	protected void process() throws Exception {
		String nodeId = this.processCond();
		this.getSlot().setCondResult(this.getClass().getName(), nodeId);
	}
	
	protected abstract String processCond() throws Exception;

}
