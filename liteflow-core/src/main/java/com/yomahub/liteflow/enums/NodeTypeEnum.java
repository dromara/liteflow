package com.yomahub.liteflow.enums;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.annotation.LiteflowCmpDefine;
import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.*;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.core.proxy.LiteFlowProxyUtil;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 节点类型枚举
 *
 * @author Bryan.Zhang
 * @since 2.6.0
 */
public enum NodeTypeEnum {

	COMMON("common", "普通", false, NodeComponent.class),

	SWITCH("switch", "选择", false, NodeSwitchComponent.class),

	BOOLEAN("boolean", "布尔", false, NodeBooleanComponent.class),

	FOR("for", "循环次数", false, NodeForComponent.class),

	ITERATOR("iterator", "循环迭代", false, NodeIteratorComponent.class),

	SCRIPT("script", "脚本", true, ScriptCommonComponent.class),

	SWITCH_SCRIPT("switch_script", "选择脚本", true, ScriptSwitchComponent.class),

	BOOLEAN_SCRIPT("boolean_script", "布尔脚本", true, ScriptBooleanComponent.class),

	FOR_SCRIPT("for_script", "循环次数脚本", true, ScriptForComponent.class),

	FALLBACK("fallback", "降级", false, null);

	private static final LFLog LOG = LFLoggerManager.getLogger(NodeTypeEnum.class);

	private String code;

	private String name;

	private boolean isScript;

	private Class<? extends NodeComponent> mappingClazz;

	NodeTypeEnum(String code, String name, boolean isScript, Class<? extends NodeComponent> mappingClazz) {
		this.code = code;
		this.name = name;
		this.isScript = isScript;
		this.mappingClazz = mappingClazz;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isScript() {
		return isScript;
	}

	public void setScript(boolean script) {
		isScript = script;
	}

	public Class<? extends NodeComponent> getMappingClazz() {
		return mappingClazz;
	}

	public void setMappingClazz(Class<? extends NodeComponent> mappingClazz) {
		this.mappingClazz = mappingClazz;
	}

	public static NodeTypeEnum getEnumByCode(String code) {
		for (NodeTypeEnum e : NodeTypeEnum.values()) {
			if (e.getCode().equals(code)) {
				return e;
			}
		}
		return null;
	}

	public static NodeTypeEnum guessTypeBySuperClazz(Class<?> clazz) {
		Class<?> superClazz = clazz;
		while (true) {
			superClazz = superClazz.getSuperclass();
			if (superClazz.getPackage().getName().startsWith("com.yomahub.liteflow.core")) {
				break;
			}
			if (superClazz.equals(Object.class)) {
				return null;
			}
		}

		for (NodeTypeEnum e : NodeTypeEnum.values()) {
			if (e.getMappingClazz() != null && e.getMappingClazz().equals(superClazz)) {
				return e;
			}
		}
		return null;
	}

	public static NodeTypeEnum guessType(Class<?> clazz) {
		if (LiteFlowProxyUtil.isCglibProxyClass(clazz)) {
			clazz = LiteFlowProxyUtil.getUserClass(clazz);
		}

		NodeTypeEnum nodeType = guessTypeBySuperClazz(clazz);
		if (nodeType == null) {
			// 尝试从类声明处进行推断
			LiteflowCmpDefine liteflowCmpDefine = clazz.getAnnotation(LiteflowCmpDefine.class);
			if (liteflowCmpDefine != null) {
				// 类声明方式中@LiteflowMethod是无需设置nodeId的
				// 但是如果设置了，那么核心逻辑其实是取类上定义的id的
				// 这种可以运行，但是理解起来不大好理解，所以给出提示，建议不要这么做
				boolean mixDefined = Arrays.stream(clazz.getDeclaredMethods()).anyMatch(method -> {
					LiteflowMethod liteflowMethod = AnnotationUtil.getAnnotation(method, LiteflowMethod.class);
					if (liteflowMethod != null) {
						return StrUtil.isNotBlank(liteflowMethod.nodeId());
					}
					else {
						return false;
					}
				});

				if (mixDefined) {
					LOG.warn(
							"[[[WARNING!!!]]]The @liteflowMethod in the class[{}] defined by @liteflowCmpDefine should not configure the nodeId again!",
							clazz.getName());
				}

				// 在返回之前，还要对方法级别的@LiteflowMethod进行检查，如果存在方法上的类型与类上的不一致时，给予警告信息
				AtomicReference<Method> differenceTypeMethod = new AtomicReference<>();
				boolean hasDifferenceNodeType = Arrays.stream(clazz.getDeclaredMethods()).anyMatch(method -> {
					LiteflowMethod liteflowMethod = AnnotationUtil.getAnnotation(method, LiteflowMethod.class);
					if (liteflowMethod != null) {
						if (!liteflowMethod.nodeType().equals(liteflowCmpDefine.value())) {
							differenceTypeMethod.set(method);
							return true;
						}
						else {
							return false;
						}
					}
					else {
						return false;
					}
				});

				// 表示存在不一样的类型
				if (hasDifferenceNodeType) {
					LOG.warn(
							"[[[WARNING!!!]]]The nodeType in @liteflowCmpDefine declared on the class[{}] does not match the nodeType in @liteflowMethod declared on the method[{}]!",
							clazz.getName(), differenceTypeMethod.get().getName());
				}

				return liteflowCmpDefine.value();
			}

			// 再尝试声明式组件这部分的推断
			LiteflowMethod liteflowMethod = Arrays.stream(clazz.getDeclaredMethods())
				.map(method -> AnnotationUtil.getAnnotation(method, LiteflowMethod.class))
				.filter(Objects::nonNull)
				.filter(lfMethod -> lfMethod.value().isMainMethod())
				.findFirst()
				.orElse(null);

			if (liteflowMethod != null) {
				nodeType = liteflowMethod.nodeType();
			}
		}
		return nodeType;
	}

}
