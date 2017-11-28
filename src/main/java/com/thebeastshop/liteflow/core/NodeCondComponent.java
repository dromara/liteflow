/**
 * <p>Title: litis</p>
 * <p>Description: redis的全方位开发运维平台</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email 47483522@qq.com
 * @Date 2017-11-28
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
