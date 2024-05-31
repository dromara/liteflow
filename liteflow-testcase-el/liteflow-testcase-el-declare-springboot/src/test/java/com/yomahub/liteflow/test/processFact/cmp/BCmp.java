/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.processFact.cmp;

import com.yomahub.liteflow.annotation.LiteflowFact;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.test.processFact.context.Company;
import org.springframework.stereotype.Component;

@Component("b")
public class BCmp {

	@LiteflowMethod(value = LiteFlowMethodEnum.PROCESS, nodeType = NodeTypeEnum.COMMON)
	public void process(NodeComponent bindCmp,
						@LiteflowFact("user.company") Company company,
						@LiteflowFact("data2") Integer data) {
		company.setHeadCount(20);
	}

}
