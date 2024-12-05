package com.yomahub.liteflow.flow.element;

import com.yomahub.liteflow.enums.ExecuteableTypeEnum;

/**
 * 可执行器接口 目前实现这个接口的有3个，Chain，Condition，Node
 *
 * @author Bryan.Zhang
 */
public interface Executable{

	void execute(Integer slotIndex) throws Exception;

	default boolean isAccess(Integer slotIndex) throws Exception {
		return true;
	}

	ExecuteableTypeEnum getExecuteType();

	/**
	 * @return executeName or id
	 * @deprecated 请使用 {@link #getId()}
	 */
	@Deprecated
	default String getExecuteName() {
		return getId();
	}

	/**
	 * @param currentChainName currentChainName
	 * @deprecated 请使用 {@link #setCurrChainId(String)}
	 */
	default void setCurrChainName(String currentChainName) {
		setCurrChainId(currentChainName);
	}

	void setId(String id);

	String getId();

	void setTag(String tag);

	String getTag();

	default void setCurrChainId(String currentChainId) {

	}

	default <T> T getItemResultMetaValue(Integer slotIndex){
		return null;
	}
}
