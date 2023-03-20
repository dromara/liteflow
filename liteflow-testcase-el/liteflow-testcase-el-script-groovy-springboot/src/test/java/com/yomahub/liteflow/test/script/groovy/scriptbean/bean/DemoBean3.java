package com.yomahub.liteflow.test.script.groovy.scriptbean.bean;

import com.yomahub.liteflow.script.annotation.ScriptBean;
import org.springframework.stereotype.Component;

@Component
@ScriptBean(name = "demo3", includeMethodName = { "test1", "test2" })
public class DemoBean3 {

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
