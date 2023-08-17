/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/10/22
 */
package com.yomahub.liteflow.aop;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.Slot;

/**
 * 全局组件拦截器接口 实现这个接口并注入到spring上下文即可
 *
 * @author Bryan.Zhang
 */
public interface ICmpAroundAspect {

	void beforeProcess(NodeComponent cmp);

	void afterProcess(NodeComponent cmp);

	void onSuccess(NodeComponent cmp);

	void onError(NodeComponent cmp, Exception e);

}
