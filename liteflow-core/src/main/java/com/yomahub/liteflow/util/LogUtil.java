package com.yomahub.liteflow.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author 惊云
 * @date 2022/6/12 11:05
 */
public class LogUtil {

    private static final String EMPTY = "";

    /**
     * 日志输出，过滤掉null值的占位符
     * @param logger
     * @param message
     * @param arguments
     */
    public static void info(Logger logger,String message,Object... arguments){
        info(PlaceholderEnum.PACK_PLACEHOLDER,logger,message,arguments);
    }

    public static void info(PlaceholderEnum placeholderEnum,Logger logger,String message,Object... arguments){
        StringBuilder result = new StringBuilder(message);
        int replaceCount = 0;
        for (int i = 0; i < arguments.length; i++){
            if(Objects.isNull(arguments[i])){
                int index = StringUtils.ordinalIndexOf(result.toString(), placeholderEnum.getValue(), i+1-replaceCount);
                if(index > 0){
                    replaceCount++;
                    result.replace(index,index + placeholderEnum.getValue().length(),EMPTY);
                }
            }
        }
        Object[] objects = Arrays.stream(arguments).filter(Objects::nonNull).toArray();
        logger.info(result.toString(),objects);
    }

    public enum PlaceholderEnum{
        PLACEHOLDER("PLACEHOLDER","{}"),
        PACK_PLACEHOLDER("PACK_PLACEHOLDER", "[{}]");

        private String name;
        private String value;

        PlaceholderEnum(String name, String value) {
            this.name = name;
            this.value = value;
        }


        private String getName(){
            return name;
        }

        private String getValue(){
            return value;
        }
    }

}
