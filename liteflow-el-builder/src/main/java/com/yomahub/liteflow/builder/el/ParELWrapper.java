package com.yomahub.liteflow.builder.el;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
public class ParELWrapper extends ELWrapper {

    private boolean any;
    private boolean ignoreError;
    private String customThreadExecutor;
    private final List<String> mustExecuteList;

    public ParELWrapper(ELWrapper... elWrappers) {
        this.addWrapper(elWrappers);
        this.mustExecuteList = new ArrayList<>();
    }

    public ParELWrapper par(Object ... objects){
        ELWrapper[] elWrappers = ELBus.convertToNonBooleanOpt(objects);
        // 校验与或非表达式
        this.addWrapper(elWrappers);
        return this;
    }

    public ParELWrapper any(boolean any) {
        this.any = any;
        return this;
    }

    public ParELWrapper ignoreError(boolean ignoreError) {
        this.ignoreError = ignoreError;
        return this;
    }

    public ParELWrapper customThreadExecutor(String customThreadExecutor){
        this.customThreadExecutor = customThreadExecutor;
        return this;
    }

    public ParELWrapper must(String ... mustExecuteWrappers){
        this.mustExecuteList.addAll(Arrays.asList(mustExecuteWrappers));
        return this;
    }

    @Override
    public ParELWrapper tag(String tag) {
        this.setTag(tag);
        return this;
    }

    @Override
    public ParELWrapper id(String id) {
        this.setId(id);
        return this;
    }

    @Override
    public ParELWrapper data(String dataName, Object object) {
        super.data(dataName, object);
        return this;
    }

    @Override
    public ParELWrapper data(String dataName, String jsonString) {
        super.data(dataName, jsonString);
        return this;
    }

    @Override
    public ParELWrapper data(String dataName, Map<String, Object> jsonMap) {
        super.data(dataName, jsonMap);
        return this;
    }

    @Override
    public ParELWrapper bind(String key, String value) {
        super.bind(key, value);
        return this;
    }

    @Override
    public ParELWrapper maxWaitSeconds(Integer maxWaitSeconds){
        setMaxWaitSeconds(maxWaitSeconds);
        return this;
    }

    public ParELWrapper retry(Integer count){
        super.retry(count);
        return this;
    }

    public ParELWrapper retry(Integer count, String... exceptions){
        super.retry(count, exceptions);
        return this;
    }

    @Override
    protected String toEL(Integer depth, StringBuilder paramContext) {
        Integer sonDepth = depth == null ? null : depth + 1;
        StringBuilder sb = new StringBuilder();

        processWrapperTabs(sb, depth);
        sb.append("PAR(");
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
            // 校验must 语义与 any语义冲突
            if (this.any){
                throw new IllegalArgumentException("'.must()' and '.any()' can use in when component at the same time!");
            }
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

        // 处理公共属性输出
        processWrapperProperty(sb, paramContext);
        return sb.toString();
    }
}
