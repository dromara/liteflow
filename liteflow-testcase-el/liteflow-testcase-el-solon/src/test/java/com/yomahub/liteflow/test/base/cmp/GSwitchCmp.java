/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.base.cmp;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import org.noear.solon.annotation.Component;

@Component("g")
public class GSwitchCmp extends NodeSwitchComponent {

	@Override
	public String processSwitch() throws Exception {
		return "then_1001";
	}
}
