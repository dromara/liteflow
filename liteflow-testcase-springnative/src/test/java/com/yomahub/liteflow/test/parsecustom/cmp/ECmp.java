/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author dongguo.tao
 * @email taodongguo@foxmail.com
 * @Date 2020/4/7
 */
package com.yomahub.liteflow.test.parsecustom.cmp;

import com.yomahub.liteflow.core.NodeCondComponent;
import org.springframework.stereotype.Component;

@Component("e")
public class ECmp extends NodeCondComponent {

	@Override
	public String processCond() throws Exception {
		return "g";
	}
}
