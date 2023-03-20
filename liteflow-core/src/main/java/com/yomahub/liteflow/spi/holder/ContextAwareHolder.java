package com.yomahub.liteflow.spi.holder;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.spi.ContextAware;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 环境容器SPI工厂类 在spring体系下会获得spring的上下文包装实现
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public class ContextAwareHolder {

	private static ContextAware contextAware;

	public static ContextAware loadContextAware() {
		if (ObjectUtil.isNull(contextAware)) {
			List<ContextAware> list = new ArrayList<>();
			ServiceLoader.load(ContextAware.class).forEach(list::add);
			list.sort(Comparator.comparingInt(ContextAware::priority));
			contextAware = list.get(0);
		}
		return contextAware;
	}

	public static void clean() {
		contextAware = null;
	}

}
