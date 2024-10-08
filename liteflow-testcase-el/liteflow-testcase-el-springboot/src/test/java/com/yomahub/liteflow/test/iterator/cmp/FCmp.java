
package com.yomahub.liteflow.test.iterator.cmp;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

@Component("f")
public class FCmp extends NodeComponent {

	@Override
	public void process() throws Exception {
		Object obj1 = this.getPreLoopObj();
		Object obj2 = this.getCurrLoopObj();
		System.out.println(StrUtil.format("{}{}", obj1, obj2));
	}
}
