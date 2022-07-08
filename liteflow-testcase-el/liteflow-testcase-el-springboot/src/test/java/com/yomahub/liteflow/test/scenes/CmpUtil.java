package com.yomahub.liteflow.test.scenes;

/**
 * <p>公共方法类</p>
 * <p>node Component util</p>
 */
public class CmpUtil {

	private static final String SPLIT_STRING = "_";

	public CmpUtil() {}


    /**
     * 得到:唯一nodeId
     * @param mode 模式(枚举)
	 * @param type 类型(枚举)
     * @return String 唯一 node id
     * */
    public static String processSwitch(String mode,String type) {
        return mode + SPLIT_STRING + type;
    }
}
