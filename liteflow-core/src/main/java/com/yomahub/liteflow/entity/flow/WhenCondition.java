/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.entity.flow;

import java.util.List;

/**
 * 并行器
 * @author Bryan.Zhang
 */
public class WhenCondition extends Condition{
	/**
	 * 增加isSync属性，以区分是循序执行还是并发执行
	 */
	private boolean isASync;

	public WhenCondition(List<Executable> nodeList) {
		super(nodeList);
		isASync = true;
	}
	public WhenCondition(List<Executable> nodeList, boolean isASync) {
		super(nodeList);
		this.isASync = isASync;
	}

	public boolean isASync() {
		return isASync;
	}
}
