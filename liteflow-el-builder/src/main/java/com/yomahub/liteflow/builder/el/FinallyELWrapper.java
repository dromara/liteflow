package com.yomahub.liteflow.builder.el;

import cn.hutool.core.util.StrUtil;

import java.util.Map;

/**
 * 后置表达式
 * 只能在THEN组件中调用
 * 参数数量不限，类型不为与或非表达式
 * 支持设置 tag、id、data 属性
 *
 * @author gezuao
 * @since 2.11.1
 */
public class FinallyELWrapper extends ELWrapper {
    public FinallyELWrapper(Object ... objects){
        super.addWrapper(ELBus.convertToNonBooleanOpt(objects));
    }

    @Override
    public FinallyELWrapper tag(String tag) {
        this.setTag(tag);
        return this;
    }

    @Override
    public FinallyELWrapper id(String id) {
        this.setId(id);
        return this;
    }

    @Override
    public FinallyELWrapper data(String dataName, Object object) {
        super.data(dataName, object);
        return this;
    }

    @Override
    public FinallyELWrapper data(String dataName, String jsonString) {
        super.data(dataName, jsonString);
        return this;
    }

    @Override
    public FinallyELWrapper data(String dataName, Map<String, Object> jsonMap) {
        super.data(dataName, jsonMap);
        return this;
    }

    @Override
    public FinallyELWrapper bind(String key, String value) {
        super.bind(key, value);
        return this;
    }

    /**
     * 后置组件无法设置maxWaitSeconds属性，重载用protected修饰
     *
     * @param maxWaitSeconds 最长等待秒数
     * @return {@link FinallyELWrapper}
     */
    @Override
    protected FinallyELWrapper maxWaitSeconds(Integer maxWaitSeconds){
        setMaxWaitSeconds(maxWaitSeconds);
        return this;
    }

    public FinallyELWrapper retry(Integer count){
        super.retry(count);
        return this;
    }

    public FinallyELWrapper retry(Integer count, String... exceptions){
        super.retry(count, exceptions);
        return this;
    }

    @Override
    protected String toEL(Integer depth, StringBuilder paramContext) {
        Integer sonDepth = depth == null ? null : depth + 1;
        StringBuilder sb = new StringBuilder();

        processWrapperTabs(sb, depth);
        sb.append("FINALLY(");
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

        // 处理共用属性
        processWrapperProperty(sb, paramContext);
        return sb.toString();
    }

    /**
     * FINALLY不允许maxWaitSeconds属性，重载父类
     *
     * @param elContext    EL 上下文
     * @param paramContext 参数上下文
     */
    @Override
    protected void processWrapperProperty(StringBuilder elContext, StringBuilder paramContext){
        if(this.getId() != null){
            elContext.append(StrUtil.format(".id(\"{}\")", this.getId()));
        }
        if(this.getTag() != null){
            elContext.append(StrUtil.format(".tag(\"{}\")", this.getTag()));
        }
    }
}
