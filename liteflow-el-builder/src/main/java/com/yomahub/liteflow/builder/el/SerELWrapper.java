package com.yomahub.liteflow.builder.el;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 串行组件
 * 允许调用PRE、FINALLY任意次
 * 参数数量任意，参数类型为非与或非表达式
 *
 * 支持定义 id tag data maxWaitSeconds属性
 *
 * @author gezuao
 * @since 2.11.1
 */
public class SerELWrapper extends ELWrapper {
    private final List<PreELWrapper> preELWrapperList;
    private final List<FinallyELWrapper> finallyELWrapperList;

    public SerELWrapper(ELWrapper... elWrappers) {
        preELWrapperList = new ArrayList<>();
        finallyELWrapperList = new ArrayList<>();
        this.addWrapper(elWrappers);
    }

    public SerELWrapper ser(Object ... objects){
        ELWrapper[] elWrappers = ELBus.convertToNonBooleanOpt(objects);
        // 校验与或非表达式
        this.addWrapper(elWrappers);
        return this;
    }

    protected void addPreElWrapper(PreELWrapper preElWrapper){
        this.preELWrapperList.add(preElWrapper);
    }

    protected void addFinallyElWrapper(FinallyELWrapper finallyElWrapper){
        this.finallyELWrapperList.add(finallyElWrapper);
    }

    /**
     * 在当前串行组件下创建前置组件
     *
     * @param objects 前置组件的子组件
     * @return {@link SerELWrapper}
     */
    public SerELWrapper pre(Object ... objects){
        addPreElWrapper(new PreELWrapper(objects));
        return this;
    }

    /**
     * 在当前串行组件下创建前置组件
     *
     * @param objects 后置组件的子组件
     * @return {@link SerELWrapper}
     */
    public SerELWrapper finallyOpt(Object ... objects){
        addFinallyElWrapper(new FinallyELWrapper(objects));
        return this;
    }

    @Override
    public SerELWrapper tag(String tag) {
        this.setTag(tag);
        return this;
    }

    @Override
    public SerELWrapper id(String id) {
        this.setId(id);
        return this;
    }

    @Override
    public SerELWrapper data(String dataName, Object object) {
        super.data(dataName, object);
        return this;
    }

    @Override
    public SerELWrapper data(String dataName, String jsonString) {
        super.data(dataName, jsonString);
        return this;
    }

    @Override
    public SerELWrapper data(String dataName, Map<String, Object> jsonMap) {
        super.data(dataName, jsonMap);
        return this;
    }

    @Override
    public SerELWrapper bind(String key, String value) {
        super.bind(key, value);
        return this;
    }

    @Override
    public SerELWrapper maxWaitSeconds(Integer maxWaitSeconds){
        setMaxWaitSeconds(maxWaitSeconds);
        return this;
    }

    public SerELWrapper retry(Integer count){
        super.retry(count);
        return this;
    }

    public SerELWrapper retry(Integer count, String... exceptions){
        super.retry(count, exceptions);
        return this;
    }

    @Override
    protected String toEL(Integer depth, StringBuilder paramContext) {
        Integer sonDepth = depth == null ? null : depth + 1;
        StringBuilder sb = new StringBuilder();

        processWrapperTabs(sb, depth);
        sb.append("SER(");
        processWrapperNewLine(sb, depth);

        // 处理前置组件输出
        for (PreELWrapper preElWrapper : this.preELWrapperList) {
            sb.append(StrUtil.format("{},", preElWrapper.toEL(sonDepth, paramContext)));
            processWrapperNewLine(sb, depth);
        }
        // 处理子表达式输出
        for (int i = 0; i < this.getElWrapperList().size(); i++) {
            if (i > 0){
                sb.append(",");
                processWrapperNewLine(sb, depth);
            }
            sb.append(this.getElWrapperList().get(i).toEL(sonDepth, paramContext));
        }
        // 处理后置组件输出
        for (FinallyELWrapper finallyElWrapper : this.finallyELWrapperList) {
            sb.append(",");
            processWrapperNewLine(sb, depth);
            sb.append(finallyElWrapper.toEL(sonDepth, paramContext));
        }
        processWrapperNewLine(sb, depth);
        processWrapperTabs(sb, depth);
        sb.append(")");

        // 处理公共属性输出
        processWrapperProperty(sb, paramContext);
        return sb.toString();
    }


}
