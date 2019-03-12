/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-7-28
 * @version 1.0
 */
package com.thebeastshop.liteflow.entity.config;

import java.util.List;

public class WhenCondition extends Condition{

	public WhenCondition(List<Node> nodeList) {
		super(nodeList);
	}

}
