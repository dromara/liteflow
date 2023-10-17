package com.yomahub.liteflow.test.builder.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.builder.vo.User;
import org.springframework.stereotype.Component;

/**
 * EL表达式装配并执行测试
 *
 * @author gezuao
 * @since 2.11.1
 */
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
