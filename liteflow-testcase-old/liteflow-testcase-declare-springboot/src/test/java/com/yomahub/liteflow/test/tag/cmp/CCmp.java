/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.tag.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowSwitchCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;

@LiteflowComponent("c")
@LiteflowSwitchCmpDefine
public class CCmp{

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS_COND)
	public String processCond(NodeComponent bindCmp) throws Exception {
		if(bindCmp.getTag().equals("2")){
			return "e";
		}else{
			return "d";
		}
	}
}
