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

	public WhenCondition(List<Executable> nodeList) {
		super(nodeList);
	}
}
