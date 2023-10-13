package com.yomahub.liteflow.builder.el;

import com.yomahub.liteflow.util.JsonUtil;

import java.util.Map;

/**
 * 与或非表达式中的 与表达式
 * 参数允许任意数量 参数必须返回true或false
 *
 * 使用and()方法加入新的表达式
 * 支持设置 id tag data maxWaitSeconds 属性
 *
 * @author gezuao
 * @since 2.11.1
 */
public class AndELWrapper extends ELWrapper {

    public AndELWrapper(ELWrapper... elWrappers){
        this.addWrapper(elWrappers);
    }

    public AndELWrapper and(Object ... object){
        ELWrapper[] wrapper = ELBus.convertToLogicOpt(object);
        this.addWrapper(wrapper);
        return this;
    }

    @Override
    public AndELWrapper tag(String tag) {
        this.setTag(tag);
        return this;
    }

    @Override
    public AndELWrapper id(String id) {
        this.setId(id);
        return this;
    }

    @Override
    public AndELWrapper data(String dataName, Object object) {
        setData(JsonUtil.toJsonString(object));
        setDataName(dataName);
        return this;
    }

    @Override
    public AndELWrapper data(String dataName, String jsonString) {
        // 校验字符串符合Json格式
        try {
            JsonUtil.parseObject(jsonString);
        } catch (Exception e){
            throw new RuntimeException("字符串不符合Json格式！");
        }
        setData(jsonString);
        setDataName(dataName);
        return this;
    }

    @Override
    public AndELWrapper data(String dataName, Map<String, Object> jsonMap) {
        setData(JsonUtil.toJsonString(jsonMap));
        setDataName(dataName);
        return this;
    }

    @Override
    protected AndELWrapper maxWaitSeconds(Integer maxWaitSeconds){
        setMaxWaitSeconds(maxWaitSeconds);
        return this;
    }

    @Override
    protected String toEL(Integer depth, StringBuilder paramContext) {
        // 根据depth是否为null，决定输出是否格式化
        Integer sonDepth = depth == null ? null : depth + 1;
        StringBuilder sb = new StringBuilder();

        processWrapperTabs(sb, depth);
        sb.append("AND(");
        processWrapperNewLine(sb, depth);
        // 处理子表达式的输出并串接
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

        // 设置共有属性
        processWrapperProperty(sb, paramContext);
        return sb.toString();
    }
}
