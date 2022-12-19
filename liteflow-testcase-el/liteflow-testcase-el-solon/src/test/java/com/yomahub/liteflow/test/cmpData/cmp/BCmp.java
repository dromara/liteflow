/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.cmpData.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.cmpData.vo.User;
import org.noear.solon.annotation.Component;

@Component("b")
public class BCmp extends NodeComponent {

	@Override
	public void process() {
		User user = this.getCmpData(User.class);
		DefaultContext context = this.getFirstContextBean();
		context.setData("user", user);
		System.out.println("BCmp executed!");
	}

}
