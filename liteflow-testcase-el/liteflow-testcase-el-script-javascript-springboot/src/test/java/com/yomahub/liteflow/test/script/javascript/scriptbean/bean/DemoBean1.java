package com.yomahub.liteflow.test.script.javascript.scriptbean.bean;

import com.yomahub.liteflow.script.annotation.ScriptBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@ScriptBean("demo")
public class DemoBean1 {

	@Resource
	private DemoBean2 demoBean2;

	public String getDemoStr1() {
		return "hello";
	}

	public String getDemoStr2(String name) {
		return demoBean2.getDemoStr2(name);
	}

}
