package com.yomahub.liteflow.builder.el;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.util.JsonUtil;

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
 * @date 2023/10/10
 * @since 2.11.1
 */
public class ThenELWrapper extends ELWrapper {
    private final List<PreELWrapper> preELWrapperList;
    private final List<FinallyELWrapper> finallyELWrapperList;

    public ThenELWrapper(ELWrapper ... elWrappers) {
        preELWrapperList = new ArrayList<>();
        finallyELWrapperList = new ArrayList<>();
        this.addWrapper(elWrappers);
    }

    public ThenELWrapper then(Object ... objects){
        ELWrapper[] elWrappers = ELBus.convertToNonLogicOpt(objects);
        // 校验与或非表达式
        this.addWrapper(elWrappers);
        return this;
    }

    protected void addPreELWrapper(PreELWrapper preELWrapper){
        this.preELWrapperList.add(preELWrapper);
    }

    protected void addFinallyELWrapper(FinallyELWrapper finallyELWrapper){
        this.finallyELWrapperList.add(finallyELWrapper);
    }

    /**
     * 定义子前置组件
     * @param objects
     * @return
     */
    public ThenELWrapper pre(Object ... objects){
        addPreELWrapper(new PreELWrapper(objects));
        return this;
    }

    /**
     * 定义子后置组件
     * @param objects
     * @return
     */
    public ThenELWrapper finallyOpt(Object ... objects){
        addFinallyELWrapper(new FinallyELWrapper(objects));
        return this;
    }

    @Override
    public ThenELWrapper tag(String tag) {
        this.setTag(tag);
        return this;
    }

    @Override
    public ThenELWrapper id(String id) {
        this.setId(id);
        return this;
    }

    // data关键字的约束：允许以Bean、jsonString、map类型输入数据，必须包含dataName参数。
    @Override
    public ThenELWrapper data(String dataName, Object javaBean) {
        setData(JsonUtil.toJsonString(javaBean));
        setDataName(dataName);
        return this;
    }

    @Override
    public ThenELWrapper data(String dataName, String jsonString) {
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
    public ThenELWrapper data(String dataName, Map<String, Object> jsonMap) {
        setData(JsonUtil.toJsonString(jsonMap));
        setDataName(dataName);
        return this;
    }

    @Override
    public ThenELWrapper maxWaitSeconds(Integer maxWaitSeconds){
        setMaxWaitSeconds(maxWaitSeconds);
        return this;
    }

    @Override
    protected String toEL(Integer depth, StringBuilder paramContext) {
        Integer sonDepth = depth == null ? null : depth + 1;
        StringBuilder sb = new StringBuilder();

        processWrapperTabs(sb, depth);
        sb.append("THEN(");
        processWrapperNewLine(sb, depth);

        // 处理前置组件输出
        for (PreELWrapper preELWrapper : this.preELWrapperList) {
            sb.append(StrUtil.format("{},", preELWrapper.toEL(sonDepth, paramContext)));
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
        for (FinallyELWrapper finallyELWrapper : this.finallyELWrapperList) {
            sb.append(",");
            processWrapperNewLine(sb, depth);
            sb.append(finallyELWrapper.toEL(sonDepth, paramContext));
        }
        processWrapperNewLine(sb, depth);
        processWrapperTabs(sb, depth);
        sb.append(")");

        // 处理公共属性输出
        processWrapperProperty(sb, paramContext);
        return sb.toString();
    }


}
