/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.multiContext.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.test.multiContext.CheckContext;
import org.noear.solon.annotation.Component;

@Component("a")
public class ACmp extends NodeComponent {

	@Override
	public void process() {
		CheckContext checkContext = this.getContextBean(CheckContext.class);
		checkContext.setSign("987XYZ");
		checkContext.setRandomId(95);
		System.out.println("ACmp executed!");
	}
}
