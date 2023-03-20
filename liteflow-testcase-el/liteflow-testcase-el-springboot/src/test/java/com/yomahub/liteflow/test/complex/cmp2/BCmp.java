/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.complex.cmp2;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import org.springframework.stereotype.Component;

@Component("B")
public class BCmp extends NodeSwitchComponent {

	public String processSwitch() throws Exception {
		return "t3";
	}

}
