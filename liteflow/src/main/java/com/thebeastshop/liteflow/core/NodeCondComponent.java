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

import org.springframework.stereotype.Component;

public abstract class NodeCondComponent extends NodeComponent {

	@Override
	protected void process() throws Exception {
		Class<?> clazz = this.processCond();
		Component component = clazz.getAnnotation(Component.class);
		String nodeId = component.value();
		this.getSlot().setCondResult(this.getClass().getName(), nodeId);
	}
	
	protected abstract Class<? extends NodeComponent> processCond() throws Exception;

}
