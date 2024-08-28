
package com.yomahub.liteflow.test.iterator.cmp;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

@Component("e")
public class ECmp extends NodeComponent {

	@Override
	public void process() throws Exception {
		DefaultContext context = this.getFirstContextBean();

		if(context.hasData("index_str")){
			String indexStr = context.getData("index_str").toString();
			indexStr = StrUtil.format("{}[{}{}]", indexStr, this.getPreLoopIndex(), this.getLoopIndex());
			context.setData("index_str", indexStr);
		}else{
			context.setData("index_str", StrUtil.format("[{}{}]", this.getPreLoopIndex(), this.getLoopIndex()));
		}

		if(context.hasData("obj_str")){
			String objStr = context.getData("obj_str").toString();
			objStr = StrUtil.format("{}[{}{}]", objStr, this.getPreLoopObj(), this.getCurrLoopObj());
			context.setData("obj_str", objStr);
		}else{
			context.setData("obj_str", StrUtil.format("[{}{}]", this.getPreLoopObj().toString(), this.getCurrLoopObj().toString()));
		}

		System.out.println("ECmp executed!");
	}
}
