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

@Component("c")
public class CCmp extends NodeComponent {

	@Override
	public void process() {
		OrderContext orderContext = this.getContextBean(OrderContext.class);
		orderContext.setOrderType(2);
		System.out.println("CCmp executed!");
	}

}
