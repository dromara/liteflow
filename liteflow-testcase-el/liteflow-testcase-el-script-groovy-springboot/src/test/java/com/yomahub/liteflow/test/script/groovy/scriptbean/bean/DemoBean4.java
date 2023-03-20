package com.yomahub.liteflow.test.script.groovy.scriptbean.bean;

import com.yomahub.liteflow.script.annotation.ScriptBean;
import org.springframework.stereotype.Component;

@Component
@ScriptBean(name = "demo4", excludeMethodName = { "test2", "test3" })
public class DemoBean4 {

	public String test1(String name) {
		return "hello," + name;
	}

	public String test2(String name) {
		return "hello," + name;
	}

	public String test3(String name) {
		return "hello," + name;
	}

}
