/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.searchContext.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

@Component("a1")
public class A1Cmp extends NodeComponent {

	@Override
	public void process() {
		String name = this.getContextValue("name");

		this.setContextValue("setName", "hello," + name);

	}
}
