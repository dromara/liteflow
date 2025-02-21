/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.bindData.cmp2;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

@Component("a4")
public class A4Cmp extends NodeComponent {

	@Override
	public void process() {
		DefaultContext context = this.getContextBean(DefaultContext.class);
		String bindValue = this.getBindData("k1", String.class);
		if (bindValue != null) {
			context.setData(this.getNodeId(), bindValue);
		}
	}

}
