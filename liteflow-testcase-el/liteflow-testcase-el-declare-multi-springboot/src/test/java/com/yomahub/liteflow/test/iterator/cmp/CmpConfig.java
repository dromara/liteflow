package com.yomahub.liteflow.test.iterator.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.base.cmp.TestDomain;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.List;

@LiteflowComponent
public class CmpConfig {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeId = "a")
	public void processA(NodeComponent bindCmp) {
		String key = "test";
		DefaultContext context = bindCmp.getFirstContextBean();
		if (!context.hasData(key)) {
			context.setData(key, bindCmp.getCurrLoopObj());
		}
		else {
			String str = context.getData(key);
			str += bindCmp.getCurrLoopObj();
			context.setData(key, str);
		}
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_BOOLEAN, nodeId = "b", nodeType = NodeTypeEnum.BOOLEAN)
	public boolean processB(NodeComponent bindCmp) {
		return bindCmp.getLoopIndex() == 1;
	}

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS_ITERATOR, nodeId = "it", nodeType = NodeTypeEnum.ITERATOR)
	public Iterator<?> processIT(NodeComponent bindCmp) {
		List<String> list = bindCmp.getRequestData();
		return list.iterator();
	}

}
