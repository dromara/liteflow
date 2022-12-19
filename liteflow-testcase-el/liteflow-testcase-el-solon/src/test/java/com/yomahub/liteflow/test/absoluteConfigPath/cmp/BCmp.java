/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.absoluteConfigPath.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.noear.solon.annotation.Component;

@Component("b")
public class BCmp extends NodeComponent {

	@Override
	public void process() {
		System.out.println("BCmp executed!");
	}

}
