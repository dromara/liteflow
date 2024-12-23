/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.instanceIds.cmp;

import com.yomahub.liteflow.core.NodeBooleanComponent;
import org.springframework.stereotype.Component;

@Component("x2")
public class X2Cmp extends NodeBooleanComponent {

	@Override
	public boolean processBoolean() throws Exception {
		return true;
	}

	@Override
	public boolean isAccess() {
		return false;
	}

}
