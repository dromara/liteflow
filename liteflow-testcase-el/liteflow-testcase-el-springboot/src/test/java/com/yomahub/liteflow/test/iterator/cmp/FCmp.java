
package com.yomahub.liteflow.test.iterator.cmp;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component("f")
public class FCmp extends NodeComponent {

	@Override
	public void process() throws Exception {
		Object obj1 = this.getPreLoopObj();
		Object obj2 = this.getCurrLoopObj();
		if (obj1 == null || obj2 == null) {
			throw new RuntimeException("");
		}
		String str = StrUtil.format("{}{}", obj1, obj2);
		DefaultContext context = this.getFirstContextBean();
		Set<String> set = context.getData("set");
		set.add(str);
	}
}
