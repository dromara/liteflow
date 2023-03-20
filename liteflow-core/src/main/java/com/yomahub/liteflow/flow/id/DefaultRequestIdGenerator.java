package com.yomahub.liteflow.flow.id;

import cn.hutool.core.util.IdUtil;

/**
 * 默认 Id 生成器
 *
 * @author tangkc
 */
public class DefaultRequestIdGenerator implements RequestIdGenerator {

	@Override
	public String generate() {
		return IdUtil.fastSimpleUUID();
	}

}
