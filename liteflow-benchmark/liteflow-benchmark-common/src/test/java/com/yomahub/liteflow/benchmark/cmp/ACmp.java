/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.benchmark.cmp;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;

import java.util.List;

@LiteflowComponent("a")
public class ACmp extends NodeComponent {

	@Override
	public void process() {
		int v1 = 2;
		int v2 = 3;
		DefaultContext ctx = this.getFirstContextBean();
		ctx.setData("s1", v1 * v2);

		TestDomain domain = ContextAwareHolder.loadContextAware().getBean(TestDomain.class);
		String str = domain.sayHello("jack");
		ctx.setData("hi", str);

		List<Person> personList = ListUtil.toList(
				new Person("jack", 15000),
				new Person("tom", 13500),
				new Person("peter", 18600)
		);

		int totalSalary = personList.stream().mapToInt(Person::getSalary).sum();

		ctx.setData("salary", totalSalary);
	}

}
