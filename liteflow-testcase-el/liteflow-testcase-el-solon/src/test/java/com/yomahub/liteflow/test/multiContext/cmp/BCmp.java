/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.multiContext.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.test.multiContext.OrderContext;
import org.noear.solon.annotation.Component;

@Component("b")
public class BCmp extends NodeComponent {

	@Override
	public void process() {
		//getContextBean无参方法是获取到第一个上下文
		OrderContext orderContext = this.getFirstContextBean();
		orderContext.setOrderNo("SO12345");
		System.out.println("BCmp executed!");
	}

}
