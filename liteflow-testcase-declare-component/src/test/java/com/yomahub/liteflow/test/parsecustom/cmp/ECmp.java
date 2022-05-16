/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.parsecustom.cmp;

import com.yomahub.liteflow.annotation.LiteflowCondCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.core.NodeCondComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import org.springframework.stereotype.Component;

@Component("e")
@LiteflowCondCmpDefine
public class ECmp{

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS_COND)
	public String processCond(NodeComponent bindCmp) throws Exception {
		return "g";
	}
}
