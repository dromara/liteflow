/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/10/22
 */
package com.yomahub.liteflow.aop;

import com.yomahub.liteflow.slot.Slot;

/**
 * 全局组件拦截器接口 实现这个接口并注入到spring上下文即可
 *
 * @author Bryan.Zhang
 */
public interface ICmpAroundAspect {

	/**
	 * 前置处理
	 * @param nodeId 节点ID
	 * @param slot
	 */
	void beforeProcess(String nodeId, Slot slot);

	/**
	 * 后置处理
	 * @param nodeId 节点ID
	 * @param slot
	 */
	void afterProcess(String nodeId, Slot slot);

}
