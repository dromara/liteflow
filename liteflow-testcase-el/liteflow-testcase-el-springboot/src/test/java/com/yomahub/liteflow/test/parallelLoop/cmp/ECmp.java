/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.parallelLoop.cmp;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

@Component("e")
public class ECmp extends NodeComponent{

	//注意与串行的ECmp相比,并行的ECmp的process方法必须保证线程安全
	@Override
	public synchronized void process() {
		DefaultContext context = this.getFirstContextBean();
		String key = StrUtil.format("{}_{}", "loop", this.getTag());
		if (context.hasData(key)) {
			String loopStr = context.getData(key);
			String loopStrReturn = StrUtil.format("{}{}", loopStr, this.getLoopIndex());
			context.setData(key, loopStrReturn);
		}
		else {
			context.setData(key, this.getLoopIndex().toString());
		}
	}

}
