package com.yomahub.liteflow.spi.spring;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.spi.PathContentParser;
import com.yomahub.liteflow.util.PathMatchUtil;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SpringPathContentParser implements PathContentParser {

	@Override
	public List<String> parseContent(List<String> pathList) throws Exception {
		List<String> absolutePathList = PathMatchUtil.searchAbsolutePath(pathList);
		List<Resource> allResource = getResources(absolutePathList);
		verifyFileExtName(allResource);

		// 转换成内容List
		List<String> contentList = new ArrayList<>();
		for (Resource resource : allResource) {
			String content = IoUtil.read(resource.getInputStream(), CharsetUtil.CHARSET_UTF_8);
			if (StrUtil.isNotBlank(content)) {
				contentList.add(content);
			}
		}

		return contentList;
	}

	@Override
	public List<String> getFileAbsolutePath(List<String> pathList) throws Exception {
		List<String> absolutePathList = PathMatchUtil.searchAbsolutePath(pathList);
		List<Resource> allResource = getResources(absolutePathList);

		return StreamUtil.of(allResource)
			// 过滤非 file 类型 Resource
			.filter(Resource::isFile)
			.map(r -> {
				try {
					return r.getFile().getAbsolutePath();
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			})
			.collect(Collectors.toList());
	}

	private List<Resource> getResources(List<String> pathList) throws IOException {
		if (CollectionUtil.isEmpty(pathList)) {
			throw new ConfigErrorException("rule source must not be null");
		}

		List<Resource> allResource = new ArrayList<>();
		for (String path : pathList) {
			String locationPattern;

			// 如果path是绝对路径且这个文件存在时，我们认为这是一个本地文件路径，而并非classpath路径
			if (FileUtil.isAbsolutePath(path) && FileUtil.isFile(path)) {
				locationPattern = ResourceUtils.FILE_URL_PREFIX + path;
			}
			else {
				if (!path.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)
						&& !path.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX)) {
					locationPattern = ResourceUtils.CLASSPATH_URL_PREFIX + path;
				}
				else {
					locationPattern = path;
				}
			}

			PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
			Resource[] resources = resolver.getResources(locationPattern);
			if (ArrayUtil.isNotEmpty(resources)) {
				allResource.addAll(ListUtil.toList(resources));
			}
		}
		return allResource;
	}

	private void verifyFileExtName(List<Resource> allResource) {
		// 检查资源都是同一个类型，如果出现不同类型的配置，则抛出错误提示
		Set<String> fileTypeSet = new HashSet<>();
		allResource.forEach(resource -> fileTypeSet.add(FileUtil.extName(resource.getFilename())));
		if (fileTypeSet.size() > 1) {
			throw new ConfigErrorException("config error,please use the same type of configuration");
		}
	}

	@Override
	public int priority() {
		return 1;
	}

}
