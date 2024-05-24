package com.yomahub.liteflow.enums;

public enum ScriptTypeEnum {

	CUSTOM("custom", "custom"),
	GROOVY("groovy", "groovy"),
	QLEXPRESS("qlexpress", "qlexpress"),
	JS("javascript", "js"),
	PYTHON("python", "python"),
	LUA("luaj", "lua"),
	AVIATOR("AviatorScript", "aviator"),
	JAVA("java", "java"),
	KOTLIN("kotlin", "kotlin");

	private String engineName;

	private String displayName;

	ScriptTypeEnum(String engineName, String displayName) {
		this.engineName = engineName;
		this.displayName = displayName;
	}

	public String getEngineName() {
		return engineName;
	}

	public void setEngineName(String engineName) {
		this.engineName = engineName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public static ScriptTypeEnum getEnumByDisplayName(String displayName) {
		for (ScriptTypeEnum e : ScriptTypeEnum.values()) {
			if (e.getDisplayName().equals(displayName)) {
				return e;
			}
		}
		return null;
	}

	/**
	 * 校验脚本类型是否合法
	 * @param scriptType 脚本类型
	 * @return true:合法，false:不合法
	 */
	public static boolean checkScriptType(String scriptType) {
		for (ScriptTypeEnum e : ScriptTypeEnum.values()) {
			if (e.getDisplayName().equals(scriptType)) {
				return true;
			}
		}
		return false;
	}

}
