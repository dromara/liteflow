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

@Component("a4")
public class A4Cmp extends NodeComponent {

	@Override
	public void process() {
		Integer id = this.getContextValue("orderContext.id");
		this.setContextValue("setData", "test", id);

	}
}
