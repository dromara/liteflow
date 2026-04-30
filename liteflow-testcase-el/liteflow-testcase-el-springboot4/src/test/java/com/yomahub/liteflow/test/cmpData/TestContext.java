package com.yomahub.liteflow.test.cmpData;

import cn.hutool.core.collection.ConcurrentHashSet;

import java.util.Set;

public class TestContext {

	private Set<String> set = new ConcurrentHashSet<>();

	public void add2Set(String str) {
		set.add(str);
	}

	public Set<String> getSet() {
		return set;
	}

}
