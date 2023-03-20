package com.yomahub.liteflow.spi.holder;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.spi.CmpAroundAspect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * 组件全局拦截器SPI工厂类
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public class CmpAroundAspectHolder {

	private static CmpAroundAspect cmpAroundAspect;

	public static CmpAroundAspect loadCmpAroundAspect() {
		if (ObjectUtil.isNull(cmpAroundAspect)) {
			List<CmpAroundAspect> list = new ArrayList<>();
			ServiceLoader.load(CmpAroundAspect.class).forEach(list::add);
			list.sort(Comparator.comparingInt(CmpAroundAspect::priority));
			cmpAroundAspect = list.get(0);
		}
		return cmpAroundAspect;
	}

	public static void clean() {
		cmpAroundAspect = null;
	}

}
