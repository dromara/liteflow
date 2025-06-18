package com.yomahub.liteflow.util;

import org.apache.commons.text.translate.AggregateTranslator;
import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.LookupTranslator;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 提供选择性的Java字符串转义功能，仅转义必要字符，保留非ASCII字符。
 * @author Bryan.Zhang
 * @since 2.13.2
 */
public final class SelectiveJavaEscaper {

    // 私有构造函数，防止实例化工具类
    private SelectiveJavaEscaper() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    /**
     * 自定义的CharSequenceTranslator，用于选择性Java转义。
     * 它转义：", \, 和标准的Java控制字符 (\n, \t, \r, \f, \b)。
     * 它保留所有其他字符，包括非ASCII字符。
     */
    public static final CharSequenceTranslator ESCAPE_JAVA_SELECTIVE;

    static {
        // 1. 定义基础转义映射 (引号和反斜杠)
        final Map<CharSequence, CharSequence> basicEscapeMap = new HashMap<>();
        basicEscapeMap.put("\"", "\\\"");
        basicEscapeMap.put("\\", "\\\\");
        final CharSequenceTranslator basicEscaper = new LookupTranslator(Collections.unmodifiableMap(basicEscapeMap));

        // 2. 定义控制字符转义映射
        // 注意: 实际项目中应验证EntityArrays或手动确保映射完整
        final Map<CharSequence, CharSequence> controlCharsMap = new HashMap<>();
        controlCharsMap.put("\n", "\\n");
        controlCharsMap.put("\t", "\\t");
        controlCharsMap.put("\r", "\\r");
        controlCharsMap.put("\f", "\\f");
        controlCharsMap.put("\b", "\\b");
        // 可以在这里添加其他需要的控制字符转义，例如垂直制表符\v (\u000B -> \\v 并不标准，通常转为\u000B)
        // 对于Java，通常只需要处理上述5个以及可能的其他ASCII控制码（< 32）
        final CharSequenceTranslator controlCharsEscaper = new LookupTranslator(Collections.unmodifiableMap(controlCharsMap));

        // 3. 使用AggregateTranslator组合转换器
        // 顺序可能影响结果，这里假设AggregateTranslator会正确处理
        // 通常将最具体的或最需要优先处理的放在前面可能更安全
        ESCAPE_JAVA_SELECTIVE = new AggregateTranslator(
                controlCharsEscaper, // 先处理 \n, \t 等
                basicEscaper         // 再处理 " 和 \
        );
        // 关键：没有添加任何形式的 UnicodeEscaper
    }

    /**
     * 对输入字符串应用选择性Java转义规则。
     *
     * @param input 要转义的字符串，可以为 null
     * @return 转义后的字符串；如果输入为 null，则返回 null
     */
    public static String escape(final String input) {
        if (input == null) {
            return null;
        }
        // StringWriter 用于高效构建结果字符串
        // 初始容量设为输入长度的1.5倍，是一个合理的估计
        StringWriter writer = new StringWriter((int) (input.length() * 1.5));
        try {
            // 执行翻译
            ESCAPE_JAVA_SELECTIVE.translate(input, writer);
        } catch (IOException ioe) {
            // 根据 StringWriter 的 Javadoc，它的 append 方法不会抛出 IOException
            // 但 translate 方法声明了可能抛出，为严谨起见进行包装
            throw new AssertionError("IOException thrown by StringWriter", ioe);
        }
        return writer.toString();
    }

    /**
     * 主方法，用于演示转义效果。
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        String originalString = "你好，\"世界\"！\n\tEnd.";
        System.out.println("原始字符串: " + originalString);

        String escapedString = SelectiveJavaEscaper.escape(originalString);
        System.out.println("选择性转义后: " + escapedString);

        // 预期输出: 你好，\"世界\"！\\n\\tEnd.
        // 验证: 中文字符被保留，引号、反斜杠、换行符、制表符被正确转义

        // 对比（可选）：使用标准 escapeJava
        // 需要导入 org.apache.commons.text.StringEscapeUtils
        // try {
        //     String defaultEscaped = org.apache.commons.text.StringEscapeUtils.escapeJava(originalString);
        //     System.out.println("标准 escapeJava: " + defaultEscaped);
        //     // 预期输出: \u4F60\u597D\uFF0C\"世界\"！\n\tEnd. (你好和，被转义)
        // } catch (NoClassDefFoundError e) {
        //     System.out.println("标准 escapeJava 未执行 (可能未取消注释或依赖问题)");
        // }
    }
}
