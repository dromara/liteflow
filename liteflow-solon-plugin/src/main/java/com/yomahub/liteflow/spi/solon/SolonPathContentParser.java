package com.yomahub.liteflow.spi.solon;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.spi.PathContentParser;
import org.noear.solon.Utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SolonPathContentParser implements PathContentParser {

	@Override
	public List<String> parseContent(List<String> pathList) throws Exception {
		List<URL> allResource = getUrls(pathList);

		// 转换成内容List
		List<String> contentList = new ArrayList<>();
		for (URL resource : allResource) {
			String content = IoUtil.read(resource.openStream(), CharsetUtil.CHARSET_UTF_8);
			if (StrUtil.isNotBlank(content)) {
				contentList.add(content);
			}
		}

		return contentList;
	}

	@Override
	public List<String> getFileAbsolutePath(List<String> pathList) throws Exception {
		List<URL> allResource = getUrls(pathList);
		return StreamUtil.of(allResource).map(URL::getPath).filter(FileUtil::isFile).collect(Collectors.toList());
	}

	private static List<URL> getUrls(List<String> pathList) throws MalformedURLException {
		if (CollectionUtil.isEmpty(pathList)) {
			throw new ConfigErrorException("rule source must not be null");
		}

		List<URL> allResource = new ArrayList<>();
		for (String path : pathList) {
			// 如果 path 是绝对路径且这个文件存在时，我们认为这是一个本地文件路径，而并非classpath路径
			if (FileUtil.isAbsolutePath(path) && FileUtil.isFile(path)) {
				allResource.add(new File(path).toURI().toURL());
			}
			else {
				if (path.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
					path = path.substring(ResourceUtils.CLASSPATH_URL_PREFIX.length());
				}

				if (Utils.getResource(path) != null) {
					allResource.add(Utils.getResource(path));
				}
			}
		}

		// 如果有多个资源，检查资源都是同一个类型，如果出现不同类型的配置，则抛出错误提示
		Set<String> fileTypeSet = new HashSet<>();
		allResource.forEach(resource -> fileTypeSet.add(FileUtil.extName(resource.getPath())));
		if (fileTypeSet.size() > 1) {
			throw new ConfigErrorException("config error,please use the same type of configuration");
		}
		return allResource;
	}

	@Override
	public int priority() {
		return 1;
	}

}
