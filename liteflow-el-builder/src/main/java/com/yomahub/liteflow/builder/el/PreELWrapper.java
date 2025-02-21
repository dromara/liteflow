package com.yomahub.liteflow.builder.el;

import com.yomahub.liteflow.util.JsonUtil;

import java.util.Map;

/**
 * 前置表达式
 * 只能在THEN组件中调用
 * 参数数量不限，类型不为与或非表达式
 * 支持设置 tag、id、data、maxWaitSeconds 属性
 *
 * @author gezuao
 * @since 2.11.1
 */
public class PreELWrapper extends ELWrapper {
    public PreELWrapper(Object ... objects){
        super.addWrapper(ELBus.convert(objects));
    }

    @Override
    public PreELWrapper tag(String tag) {
        this.setTag(tag);
        return this;
    }

    @Override
    public PreELWrapper id(String id) {
        this.setId(id);
        return this;
    }

    @Override
    public PreELWrapper data(String dataName, Object object) {
        super.data(dataName, object);
        return this;
    }

    @Override
    public PreELWrapper data(String dataName, String jsonString) {
        super.data(dataName, jsonString);
        return this;
    }

    @Override
    public PreELWrapper data(String dataName, Map<String, Object> jsonMap) {
        super.data(dataName, jsonMap);
        return this;
    }

    @Override
    public PreELWrapper bind(String key, String value) {
        super.bind(key, value);
        return this;
    }

    @Override
    public PreELWrapper maxWaitSeconds(Integer maxWaitSeconds){
        setMaxWaitSeconds(maxWaitSeconds);
        return this;
    }

    public PreELWrapper retry(Integer count){
        super.retry(count);
        return this;
    }

    public PreELWrapper retry(Integer count, String... exceptions){
        super.retry(count, exceptions);
        return this;
    }

    @Override
    protected String toEL(Integer depth, StringBuilder paramContext) {
        Integer sonDepth = depth == null ? null : depth + 1;
        StringBuilder sb = new StringBuilder();

        processWrapperTabs(sb, depth);
        sb.append("PRE(");
        processWrapperNewLine(sb, depth);
        // 处理子表达式输出
        for (int i = 0; i < this.getElWrapperList().size(); i++) {
            sb.append(this.getElWrapperList().get(i).toEL(sonDepth, paramContext));
            if (i != this.getElWrapperList().size() - 1) {
                sb.append(",");
                processWrapperNewLine(sb, depth);
            }
        }
        processWrapperNewLine(sb, depth);
        processWrapperTabs(sb, depth);
        sb.append(")");

        // 处理公共属性输出
        processWrapperProperty(sb, paramContext);
        return sb.toString();
    }
}
