package com.yomahub.liteflow.spi.holder;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.spi.PathContentParser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

public class PathContentParserHolder {

	private static PathContentParser pathContentParser;

	public static PathContentParser loadContextAware() {
		if (ObjectUtil.isNull(pathContentParser)) {
			List<PathContentParser> list = new ArrayList<>();
			ServiceLoader.load(PathContentParser.class).forEach(list::add);
			list.sort(Comparator.comparingInt(PathContentParser::priority));
			pathContentParser = list.get(0);
		}
		return pathContentParser;
	}

	public static void clean() {
		pathContentParser = null;
	}

}
