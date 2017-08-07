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

public class FComponent extends Component {

	@Override
	public void process() {
		try {
			String[] temp = new String[400];
			Thread.sleep(40L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Fcomponent executed!");
		
	}
	
}
