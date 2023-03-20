package com.yomahub.liteflow.spi.holder;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.spi.ContextCmpInit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 环境组件初始化SPI工厂类 在spring体系下会获得基于spring扫描的组件初始化实现器
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public class ContextCmpInitHolder {

	private static ContextCmpInit contextCmpInit;

	public static ContextCmpInit loadContextCmpInit() {
		if (ObjectUtil.isNull(contextCmpInit)) {
			List<ContextCmpInit> list = new ArrayList<>();
			ServiceLoader.load(ContextCmpInit.class).forEach(list::add);
			list.sort(Comparator.comparingInt(ContextCmpInit::priority));
			contextCmpInit = list.get(0);
		}
		return contextCmpInit;
	}

	public static void clean() {
		contextCmpInit = null;
	}

}
