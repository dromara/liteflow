/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.base.cmp;

import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import org.springframework.stereotype.Component;

@Component("e")
@LiteflowCmpDefine(NodeTypeEnum.COMMON)
public class ECmp {

	@LiteflowMethod(LiteFlowMethodEnum.BEFORE_PROCESS)
	public void before(NodeComponent bindCmp) throws Exception{
		int a = 1/0;
	}

	@LiteflowMethod(LiteFlowMethodEnum.PROCESS)
	public void process(NodeComponent bindCmp) {
		System.out.println("CCmp executed!");
	}

	@LiteflowMethod(LiteFlowMethodEnum.ON_ERROR)
	public void onError(NodeComponent bindCmp, Exception e) {
		if (e != null){
			e.printStackTrace();
		}else{
			System.out.println("no error");
		}

	}
}
