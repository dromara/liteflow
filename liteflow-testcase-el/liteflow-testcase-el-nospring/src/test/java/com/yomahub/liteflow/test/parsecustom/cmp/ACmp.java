/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.parsecustom.cmp;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.exception.FlowSystemException;

public class ACmp extends NodeComponent {

	@Override
	public void process() {
		String str = this.getRequestData();
		if (StrUtil.isNotBlank(str) && str.equals("exception")) {
			throw new FlowSystemException("chain execute exception");
		}
		System.out.println("ACmp executed!");
	}

}
