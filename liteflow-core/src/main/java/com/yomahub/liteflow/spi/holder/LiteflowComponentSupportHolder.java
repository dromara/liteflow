package com.yomahub.liteflow.spi.holder;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.spi.LiteflowComponentSupport;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * liteflowComponent的支持扩展SPI工厂类
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public class LiteflowComponentSupportHolder {

	private static LiteflowComponentSupport liteflowComponentSupport;

	public static LiteflowComponentSupport loadLiteflowComponentSupport() {
		if (ObjectUtil.isNull(liteflowComponentSupport)) {
			List<LiteflowComponentSupport> list = new ArrayList<>();
			ServiceLoader.load(LiteflowComponentSupport.class).forEach(list::add);
			list.sort(Comparator.comparingInt(LiteflowComponentSupport::priority));
			liteflowComponentSupport = list.get(0);
		}
		return liteflowComponentSupport;
	}

	public static void clean() {
		liteflowComponentSupport = null;
	}

}
