package com.yomahub.liteflow.test.parallelLoop.cmp;

import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.core.NodeIteratorComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Component("it")
@LiteflowCmpDefine(NodeTypeEnum.ITERATOR)
public class ITCmp {

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS_ITERATOR)
	public Iterator<?> processIterator(NodeComponent bindCmp) throws Exception {
		List<String> list = bindCmp.getRequestData();
		return list.iterator();
	}

}
