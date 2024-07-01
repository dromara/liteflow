package com.yomahub.liteflow.builder.el;

import com.yomahub.liteflow.util.JsonUtil;

import java.util.Map;

/**
 * 与或非表达式中的 非表达式
 * 参数只允许一个 参数必须返回true或false
 *
 * 支持设置 id tag data maxWaitSeconds 属性
 *
 * @author gezuao
 * @since 2.11.1
 */
public class NotELWrapper extends ELWrapper {
    public NotELWrapper(ELWrapper elWrapper){
        this.addWrapper(elWrapper);
    }

    @Override
    public NotELWrapper tag(String tag) {
        this.setTag(tag);
        return this;
    }

    @Override
    public NotELWrapper id(String id) {
        this.setId(id);
        return this;
    }

    @Override
    protected String toEL(Integer depth, StringBuilder paramContext) {
        Integer sonDepth = depth == null ? null : depth + 1;
        StringBuilder sb = new StringBuilder();

        processWrapperTabs(sb, depth);
        sb.append("NOT(");
        processWrapperNewLine(sb, depth);
        // 处理子表达式输出
        sb.append(this.getElWrapperList().get(0).toEL(sonDepth, paramContext));
        processWrapperNewLine(sb, depth);
        processWrapperTabs(sb, depth);
        sb.append(")");

        // 设置公共属性
        processWrapperProperty(sb, paramContext);
        return sb.toString();
    }
}
