package com.yomahub.liteflow.test.script.kotlin.scriptbean.bean;

import com.yomahub.liteflow.script.annotation.ScriptBean;
import org.springframework.stereotype.Component;

@Component
@ScriptBean("demo5")
public class DemoBean5 {

	private final DemoBean2 demoBean2;

	public DemoBean5(DemoBean2 demoBean2) {
		this.demoBean2 = demoBean2;
	}

	public String getDemoStr1() {
		return "hello";
	}

	public String getDemoStr2(String name) {
		return demoBean2.getDemoStr2(name);
	}

}
