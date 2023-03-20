package com.yomahub.liteflow.core;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 流程初始化的钩子类，所有的钩子都放在这里 目前钩子主要是放一些第三方中间件的规则监听 放的钩子要求都是无入参无返回的，所以这里是BooleanSupplier
 *
 * @author Bryan.Zhang
 * @since 2.9.4
 */
public class FlowInitHook {

	private static final List<BooleanSupplier> supplierList = new ArrayList<>();

	public static void executeHook() {
		if (CollUtil.isNotEmpty(supplierList)) {
			supplierList.forEach(BooleanSupplier::getAsBoolean);
		}
	}

	public static void addHook(BooleanSupplier hookSupplier) {
		supplierList.add(hookSupplier);
	}

	public static void cleanHook() {
		supplierList.clear();
	}

}
