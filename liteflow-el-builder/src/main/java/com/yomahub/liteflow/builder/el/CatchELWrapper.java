package com.yomahub.liteflow.builder.el;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import java.util.Map;

/**
 * 捕获异常表达式
 * Catch(a).do(b)
 * catch()和do()只允许单个参数
 * a，b非与或非表达式
 *
 * 支持设置 id tag data maxWaitSeconds 属性
 *
 * @author gezuao
 * @since 2.11.1
 */
public class CatchELWrapper extends ELWrapper{

    public CatchELWrapper(ELWrapper elWrapper){
        this.addWrapper(elWrapper);
    }

    public CatchELWrapper doOpt(Object object){
        ELWrapper elWrapper = ELBus.convertToNonLogicOpt(object);
        this.addWrapper(elWrapper);
        return this;
    }

    @Override
    public CatchELWrapper tag(String tag) {
        this.setTag(tag);
        return this;
    }

    @Override
    public CatchELWrapper id(String id) {
        this.setId(id);
        return this;
    }

    @Override
    public CatchELWrapper data(String dataName, Object object) {
        setData(JSONUtil.toJsonStr(object));
        setDataName(dataName);
        return this;
    }

    @Override
    public CatchELWrapper data(String dataName, String jsonString) {
        try {
            JSONUtil.parseObj(jsonString);
        } catch (Exception e){
            throw new RuntimeException("字符串不符合Json格式！");
        }
        setData(jsonString);
        setDataName(dataName);
        return this;
    }

    @Override
    public CatchELWrapper data(String dataName, Map<String, Object> jsonMap) {
        setData(JSONUtil.toJsonStr(jsonMap));
        setDataName(dataName);
        return this;
    }

    @Override
    public CatchELWrapper maxWaitSeconds(Integer maxWaitSeconds){
        setMaxWaitSeconds(maxWaitSeconds);
        return this;
    }

    @Override
    protected String toEL(Integer depth, StringBuilder paramContext) {
        Integer sonDepth = depth == null ? null : depth + 1;
        StringBuilder sb = new StringBuilder();

        // 处理 CATCH() 语句的输出
        processWrapperTabs(sb, depth);
        sb.append("CATCH(");
        processWrapperNewLine(sb, depth);
        sb.append(this.getElWrapperList().get(0).toEL(sonDepth, paramContext));
        processWrapperNewLine(sb, depth);
        processWrapperTabs(sb, depth);
        sb.append(")");

        // 处理 DO()语句输出
        if(this.getElWrapperList().size() > 1){
            sb.append(".DO(");
            processWrapperNewLine(sb, depth);
            sb.append(this.getElWrapperList().get(1).toEL(sonDepth, paramContext));
            processWrapperNewLine(sb, depth);
            processWrapperTabs(sb, depth);
            sb.append(")");
        }

        // 处理共有属性输出
        processWrapperProperty(sb, paramContext);
        return sb.toString();
    }
}
