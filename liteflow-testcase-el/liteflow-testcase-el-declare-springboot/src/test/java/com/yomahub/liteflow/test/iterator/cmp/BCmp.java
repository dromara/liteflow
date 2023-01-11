
package com.yomahub.liteflow.test.iterator.cmp;

import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeBreakComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import org.springframework.stereotype.Component;

@Component("b")
@LiteflowCmpDefine(NodeTypeEnum.BREAK)
public class BCmp{

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS_BREAK)
	public boolean processBreak(NodeComponent bindCmp) throws Exception {
		return bindCmp.getLoopIndex() == 1;
	}
}
