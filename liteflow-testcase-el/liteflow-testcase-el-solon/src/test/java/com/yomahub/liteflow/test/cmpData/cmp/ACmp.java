/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.cmpData.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.noear.solon.annotation.Component;

@Component("a")
public class ACmp extends NodeComponent {

	@Override
	public void process() {
		System.out.println(this.getCmpData(String.class));
		System.out.println("ACmp executed!");
	}
}
