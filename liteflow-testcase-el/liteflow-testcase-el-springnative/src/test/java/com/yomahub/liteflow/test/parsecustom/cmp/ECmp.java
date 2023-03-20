/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author dongguo.tao
 * @email taodongguo@foxmail.com
 * @Date 2020/4/7
 */
package com.yomahub.liteflow.test.parsecustom.cmp;

import com.yomahub.liteflow.core.NodeSwitchComponent;
import org.springframework.stereotype.Component;

@Component("e")
public class ECmp extends NodeSwitchComponent {

	@Override
	public String processSwitch() throws Exception {
		return "g";
	}

}
