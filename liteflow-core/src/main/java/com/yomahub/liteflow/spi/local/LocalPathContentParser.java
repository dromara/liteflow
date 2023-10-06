package com.yomahub.liteflow.spi.local;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.io.resource.FileResource;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ClassLoaderUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.spi.PathContentParser;
import com.yomahub.liteflow.util.PathMatchUtil;

import java.util.ArrayList;
import java.util.List;

public class LocalPathContentParser implements PathContentParser {

	private final static String FILE_URL_PREFIX = "file:";

	private final static String CLASSPATH_URL_PREFIX = "classpath:";

	@Override
	public List<String> parseContent(List<String> pathList) throws Exception {
		if (CollectionUtil.isEmpty(pathList)) {
			throw new ConfigErrorException("rule source must not be null");
		}
		List<String> absolutePathList = PathMatchUtil.searchAbsolutePath(pathList);

		List<String> contentList = new ArrayList<>();

		for (String path : absolutePathList) {
			if (FileUtil.isAbsolutePath(path) && FileUtil.isFile(path)) {
				path = FILE_URL_PREFIX + path;
			}
			else {
				if (!path.startsWith(CLASSPATH_URL_PREFIX)) {
					path = CLASSPATH_URL_PREFIX + path;
				}
			}
			String content = ResourceUtil.readUtf8Str(path);
			if (StrUtil.isNotBlank(content)) {
				contentList.add(content);
			}
		}

		return contentList;
	}

	@Override
	public List<String> getFileAbsolutePath(List<String> pathList) throws Exception {
		if (CollectionUtil.isEmpty(pathList)) {
			throw new ConfigErrorException("rule source must not be null");
		}
		List<String> absolutePathList = PathMatchUtil.searchAbsolutePath(pathList);
		List<String> result = new ArrayList<>();

		for (String path : absolutePathList) {
			if (FileUtil.isAbsolutePath(path) && FileUtil.isFile(path)) {
				path = FILE_URL_PREFIX + path;
				result.add(new FileResource(path).getFile().getAbsolutePath());
			}
			else {
				if (!path.startsWith(CLASSPATH_URL_PREFIX)) {
					path = CLASSPATH_URL_PREFIX + path;

					// 这里会有自定义解析器
					if (ClassLoaderUtil.isPresent(path)) {
						result.add(new ClassPathResource(path).getAbsolutePath());
					}
				}
			}
		}

		return result;
	}

	@Override
	public int priority() {
		return 2;
	}

}
