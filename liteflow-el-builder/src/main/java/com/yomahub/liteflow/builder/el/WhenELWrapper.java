package com.yomahub.liteflow.builder.el;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 并行组件
 * 参数数量任意
 * 参数类型非与或非表达式
 *
 * 支持定义 id tag data maxWaitSeconds 属性
 * 支持调用 any ignoreError customThreadExecutor mustExecuteList 并行组件特有方法
 * any 和 must语义冲突，不支持同时定义
 *
 * @author gezuao
 * @since 2.11.1
 */
public class WhenELWrapper extends ELWrapper {

    private boolean any;
    private boolean ignoreError;
    private String customThreadExecutor;
    private final List<String> mustExecuteList;
    private Double percentage;

    public WhenELWrapper(ELWrapper... elWrappers) {
        this.addWrapper(elWrappers);
        this.mustExecuteList = new ArrayList<>();
    }

    public WhenELWrapper when(Object ... objects){
        ELWrapper[] elWrappers = ELBus.convertToNonBooleanOpt(objects);
        // 校验与或非表达式
        this.addWrapper(elWrappers);
        return this;
    }

    public WhenELWrapper any(boolean any) {
        this.any = any;
        return this;
    }

    public WhenELWrapper percentage(double percentage) {
        this.percentage = percentage;
        return this;
    }

    public WhenELWrapper ignoreError(boolean ignoreError) {
        this.ignoreError = ignoreError;
        return this;
    }

    public WhenELWrapper customThreadExecutor(String customThreadExecutor){
        this.customThreadExecutor = customThreadExecutor;
        return this;
    }

    public WhenELWrapper must(String ... mustExecuteWrappers){
        this.mustExecuteList.addAll(Arrays.asList(mustExecuteWrappers));
        return this;
    }

    @Override
    public WhenELWrapper tag(String tag) {
        this.setTag(tag);
        return this;
    }

    @Override
    public WhenELWrapper id(String id) {
        this.setId(id);
        return this;
    }

    @Override
    public WhenELWrapper data(String dataName, Object object) {
        super.data(dataName, object);
        return this;
    }

    @Override
    public WhenELWrapper data(String dataName, String jsonString) {
        super.data(dataName, jsonString);
        return this;
    }

    @Override
    public WhenELWrapper data(String dataName, Map<String, Object> jsonMap) {
        super.data(dataName, jsonMap);
        return this;
    }

    @Override
    public WhenELWrapper bind(String key, String value) {
        super.bind(key, value);
        return this;
    }

    @Override
    public WhenELWrapper maxWaitSeconds(Integer maxWaitSeconds){
        setMaxWaitSeconds(maxWaitSeconds);
        return this;
    }

    public WhenELWrapper retry(Integer count){
        super.retry(count);
        return this;
    }

    public WhenELWrapper retry(Integer count, String... exceptions){
        super.retry(count, exceptions);
        return this;
    }

    @Override
    protected String toEL(Integer depth, StringBuilder paramContext) {

        // 互斥检查：确保三个属性中最多只有一个被设置
        long count = Stream.of(
                this.any ? 1 : 0,
                CollectionUtil.isNotEmpty(this.mustExecuteList) ? 1 : 0,
                ObjUtil.isNotNull(this.percentage) ? 1 : 0
        ).filter(num -> num > 0).count();

        if (count > 1) {
            throw new IllegalArgumentException("Properties 'any', 'must', and 'percentage' are mutually exclusive. Only one can be set at a time.");
        }

        Integer sonDepth = depth == null ? null : depth + 1;
        StringBuilder sb = new StringBuilder();

        processWrapperTabs(sb, depth);
        sb.append("WHEN(");
        processWrapperNewLine(sb, depth);
        // 处理子表达式输出
        for (int i = 0; i < this.getElWrapperList().size(); i++) {
            if (i > 0){
                sb.append(",");
                processWrapperNewLine(sb, depth);
            }
            sb.append(this.getElWrapperList().get(i).toEL(sonDepth, paramContext));
        }
        processWrapperNewLine(sb, depth);
        processWrapperTabs(sb, depth);
        sb.append(")");

        // WHEN的私有语义输出
        if (this.any){
            sb.append(".any(true)");
        }
        if(this.ignoreError){
            sb.append(".ignoreError(true)");
        }
        if(StrUtil.isNotBlank(customThreadExecutor)){
            sb.append(StrUtil.format(".threadPool(\"{}\")", customThreadExecutor));
        }
        if(CollectionUtil.isNotEmpty(mustExecuteList)){
            // 处理must子表达式输出
            sb.append(".must(");
            for(int i = 0; i < mustExecuteList.size(); i++){
                if(i > 0){
                    sb.append(", ");
                }
                sb.append(StrUtil.format("\"{}\"", mustExecuteList.get(i)));
            }
            sb.append(")");
        }

        if (ObjUtil.isNotNull(percentage)){
            sb.append(StrUtil.format(".percentage({})", percentage));
        }

        // 处理公共属性输出
        processWrapperProperty(sb, paramContext);
        return sb.toString();
    }
}
