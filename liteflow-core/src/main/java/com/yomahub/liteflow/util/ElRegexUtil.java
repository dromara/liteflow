package com.yomahub.liteflow.util;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.exception.ParseException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser 通用 Helper
 *
 * @author tangkc
 * @since 2.11.2
 */
public class ElRegexUtil {

    // java 注释的正则表达式
    private static final String REGEX_COMMENT = "(?<!(:|@))\\/\\/.*|\\/\\*(\\s|.)*?\\*\\/";

    // abstractChain 占位符正则表达式
    private static final String REGEX_ABSTRACT_HOLDER = "\\{\\{\\s*([a-zA-Z_][a-zA-Z_\\d]*|\\d+)\\s*\\}\\}(?![\\s]*=)";

    /**
     * 移除 el 表达式中的注释，支持 java 的注释，包括单行注释、多行注释， 会压缩字符串，移除空格和换行符
     *
     * @param elStr el 表达式
     * @return 移除注释后的 el 表达式
     */
    public static String removeComments(String elStr) {
        if (StrUtil.isBlank(elStr)) {
            return elStr;
        }

        return Pattern.compile(REGEX_COMMENT)
                .matcher(elStr)
                // 移除注释
                .replaceAll(CharSequenceUtil.EMPTY);
    }

    /**
     * 根据抽象EL和实现EL，替换抽象EL中的占位符
     *
     * @param abstractChain 抽象EL
     * @param implChain     抽象EL对应的一个实现
     * @return 替换后的EL
     */
    public static String replaceAbstractChain(String abstractChain, String implChain) {
        //匹配抽象chain的占位符
        Pattern placeHolder = Pattern.compile(REGEX_ABSTRACT_HOLDER);
        Matcher placeHolderMatcher = placeHolder.matcher(abstractChain);
        while (placeHolderMatcher.find()) {
            //到implChain中找到对应的占位符实现
            String holder = placeHolderMatcher.group(1);
            Pattern placeHolderImpl = Pattern.compile("\\s*\\{\\{" + holder + "\\}\\}\\s*=\\s*(.*?);");
            Matcher implMatcher = placeHolderImpl.matcher(implChain);
            if (implMatcher.find()) {
                String replacement = implMatcher.group(1).trim();
                abstractChain = abstractChain.replace("{{" + holder + "}}", replacement);
            } else {
                throw new ParseException("missing implementation of {{" + holder + "}} in expression \r\n" + implChain);
            }
        }
        return abstractChain;
    }

    /**
     * 判断某个Chain是否为抽象EL，判断依据是是否包含未实现的占位符
     *
     * @param elStr EL表达式
     * @return 判断结果，true为抽象EL，false为非抽象EL
     */
    public static boolean isAbstractChain(String elStr) {
        return Pattern.compile(REGEX_ABSTRACT_HOLDER).matcher(elStr).find();
    }
}
