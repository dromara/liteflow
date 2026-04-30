/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.loop.cmp;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

@Component("c")
public class CCmp extends NodeComponent {

	@Override
	public void process() {
		DefaultContext context = this.getFirstContextBean();

		if(context.hasData("index_str")){
			String indexStr = context.getData("index_str").toString();
			indexStr = StrUtil.format("{}[{}{}{}]", indexStr, this.getPreNLoopIndex(2), this.getPreLoopIndex(), this.getLoopIndex());
			context.setData("index_str", indexStr);
		}else{
			context.setData("index_str", StrUtil.format("[{}{}{}]", this.getPreNLoopIndex(2), this.getPreLoopIndex(), this.getLoopIndex()));
		}

		System.out.println("CCmp executed!");
	}

}
