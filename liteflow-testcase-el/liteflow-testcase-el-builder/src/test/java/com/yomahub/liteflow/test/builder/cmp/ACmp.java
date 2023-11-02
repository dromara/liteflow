package com.yomahub.liteflow.test.builder.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * EL表达式装配并执行测试
 *
 * @author gezuao
 * @since 2.11.1
 */
@Component("a")
public class ACmp extends NodeComponent {

	@Override
	public void process() {
		System.out.println(this.getCmpData(String.class));
		System.out.println("ACmp executed!");
	}

}
