
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
		Object obj1 = this.getCurrLoopObj();
		Object obj2 = this.getPreNLoopObj(1);
		Object obj3 = this.getPreNLoopObj(2);
		Object obj4 = this.getPreNLoopObj(3);
		Object obj5 = this.getPreNLoopObj(4);
		if (obj1 == null || obj2 == null) {
			throw new RuntimeException("");
		}
		String str = StrUtil.format("{}-{}-{}-{}-{}", obj5, obj4, obj3, obj2, obj1);
		DefaultContext context = this.getFirstContextBean();
		Set<String> set = context.getData("set");
		set.add(str);
	}
}
