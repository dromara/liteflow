package com.yomahub.liteflow.test.extend.cmp;

import cn.hutool.core.util.StrUtil;

public class ParentCmp {

	protected String sayHi(String name) {
		return StrUtil.format("hi,{}", name);
	}

}
