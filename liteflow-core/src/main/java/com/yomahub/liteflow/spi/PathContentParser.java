package com.yomahub.liteflow.spi;

import java.util.List;

public interface PathContentParser extends SpiPriority {

	/**
	 * 解析路径下的文件内容
	 * @param pathList 文件路径（支持 classpath 路径和 file 绝对路径，spring 环境支持
	 * PathMatchingResourcePatternResolver 规则）
	 * @return 返回文件内容
	 * @throws Exception ex
	 */
	List<String> parseContent(List<String> pathList) throws Exception;

	/**
	 * 获取文件路径的绝对路径
	 * @param pathList 文件路径（支持 classpath 路径和 file 绝对路径，spring 环境支持
	 * PathMatchingResourcePatternResolver 规则）
	 * @return 返回文件绝对路径
	 * @throws Exception ex
	 */
	List<String> getFileAbsolutePath(List<String> pathList) throws Exception;

}
