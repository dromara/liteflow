/**
 * <p>Title: liteFlow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * <p>Copyright: Copyright (c) 2017</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2017-8-1
 * @version 1.0
 */
package com.thebeastshop.liteflow.test.component;

import com.thebeastshop.liteflow.core.Component;

public class GComponent extends Component {

	@Override
	public void process() {
		System.out.println("Gcomponent executed!");
		this.getSlot().setResponseData("i am a response");
	}
	
}
