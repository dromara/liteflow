/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Tingliang Wang
 * @email bytlwang@126.com
 * @Date 2022/12/09
 */
package com.yomahub.liteflow.test.switchcase.cmp;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import org.springframework.stereotype.Component;

@Component("k")
public class KSwitchCmp extends NodeSwitchComponent {

	@Override
	public String processSwitch() throws Exception {
		return "chain10";
	}

}
