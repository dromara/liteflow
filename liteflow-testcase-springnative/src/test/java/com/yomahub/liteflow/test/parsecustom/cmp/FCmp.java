/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author dongguo.tao
 * @email taodongguo@foxmail.com
 * @Date 2020/4/7
 */
package com.yomahub.liteflow.test.parsecustom.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

@Component("f")
public class FCmp extends NodeComponent {

	@Override
	public void process() {
		System.out.println("FCmp executed!");
	}

}
