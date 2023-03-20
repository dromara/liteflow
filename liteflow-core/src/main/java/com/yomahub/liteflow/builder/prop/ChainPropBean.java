package com.yomahub.liteflow.builder.prop;

import com.yomahub.liteflow.enums.ConditionTypeEnum;

/**
 * 构建 chain 的中间属性
 */
public class ChainPropBean {

	/**
	 * 执行规则
	 */
	String condValueStr;

	/**
	 * 分组
	 */
	String group;

	/**
	 * 是否抛出异常
	 */
	String errorResume;

	/**
	 * 满足任意条件，执行完成
	 */
	String any;

	/**
	 * 指定线程池
	 */
	String threadExecutorClass;

	/**
	 * chain 类型
	 */
	ConditionTypeEnum conditionType;

	public String getCondValueStr() {
		return condValueStr;
	}

	public ChainPropBean setCondValueStr(String condValueStr) {
		this.condValueStr = condValueStr;
		return this;
	}

	public String getGroup() {
		return group;
	}

	public ChainPropBean setGroup(String group) {
		this.group = group;
		return this;
	}

	public String getErrorResume() {
		return errorResume;
	}

	public ChainPropBean setErrorResume(String errorResume) {
		this.errorResume = errorResume;
		return this;
	}

	public String getAny() {
		return any;
	}

	public ChainPropBean setAny(String any) {
		this.any = any;
		return this;
	}

	public String getThreadExecutorClass() {
		return threadExecutorClass;
	}

	public ChainPropBean setThreadExecutorClass(String threadExecutorClass) {
		this.threadExecutorClass = threadExecutorClass;
		return this;
	}

	public ConditionTypeEnum getConditionType() {
		return conditionType;
	}

	public ChainPropBean setConditionType(ConditionTypeEnum conditionType) {
		this.conditionType = conditionType;
		return this;
	}

}
