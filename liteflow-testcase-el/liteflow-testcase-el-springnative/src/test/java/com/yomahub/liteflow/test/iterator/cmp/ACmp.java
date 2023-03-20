/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.iterator.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

@Component("a")
public class ACmp extends NodeComponent {

	@Override
	public void process() {
		String key = "test";
		DefaultContext context = this.getFirstContextBean();
		if (!context.hasData(key)) {
			context.setData(key, this.getCurrLoopObj());
		}
		else {
			String str = context.getData(key);
			str += this.getCurrLoopObj();
			context.setData(key, str);
		}
	}

}
