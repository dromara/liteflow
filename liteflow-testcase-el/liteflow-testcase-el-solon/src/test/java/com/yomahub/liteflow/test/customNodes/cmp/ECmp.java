/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.customNodes.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.test.customNodes.domain.DemoDomain;
import org.noear.solon.annotation.Inject;

public class ECmp extends NodeComponent {

	@Inject
	private DemoDomain demoDomain;

	@Override
	public void process() {
		demoDomain.sayHi();
		System.out.println("ECmp executed!");
	}

}
