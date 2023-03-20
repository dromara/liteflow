package com.yomahub.liteflow.flow.id;

/**
 * Id 生成接口
 *
 * @author tangkc
 */
public interface RequestIdGenerator {

	/**
	 * 获取唯一 id
	 * @return 返回唯一 id
	 */
	String generate();

}
