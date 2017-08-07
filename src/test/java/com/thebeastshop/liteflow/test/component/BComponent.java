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

import java.util.ArrayList;
import java.util.List;

import com.thebeastshop.liteflow.core.Component;

public class BComponent extends Component {

	@Override
	public void process() {
		try {
			Thread.sleep(400L);
			String[] temp = new String[1000];
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Bcomponent executed!");
		
	}
	
}
