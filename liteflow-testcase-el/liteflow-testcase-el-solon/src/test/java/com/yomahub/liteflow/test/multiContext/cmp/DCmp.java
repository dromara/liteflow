/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.multiContext.cmp;

import cn.hutool.core.date.DateUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.test.multiContext.OrderContext;
import org.noear.solon.annotation.Component;

@Component("d")
public class DCmp extends NodeComponent {

	@Override
	public void process() {
		OrderContext orderContext = this.getContextBean(OrderContext.class);
		orderContext.setCreateTime(DateUtil.parseDate("2022-06-15"));
		System.out.println("CCmp executed!");
	}

}
