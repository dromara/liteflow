package com.yomahub.liteflow.test.builder;

import cn.hutool.core.collection.ConcurrentHashSet;

import java.util.Set;

/**
 * EL表达式装配并执行测试
 *
 * @author gezuao
 * @since 2.11.1
 */
public class TestContext {

	private Set<String> set = new ConcurrentHashSet<>();

	public void add2Set(String str) {
		set.add(str);
	}

	public Set<String> getSet() {
		return set;
	}

}
