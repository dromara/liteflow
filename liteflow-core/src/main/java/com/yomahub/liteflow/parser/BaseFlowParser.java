package com.yomahub.liteflow.parser;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.LiteFlowChainBuilder;
import com.yomahub.liteflow.builder.LiteFlowConditionBuilder;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.EmptyConditionValueException;
import com.yomahub.liteflow.exception.NodeTypeNotSupportException;
import com.yomahub.liteflow.exception.NotSupportConditionException;

/**
 * 基类，用于存放通用方法
 *
 * @author tangkc
 */
public abstract class BaseFlowParser implements FlowParser {

    /**
     * 构建 node
     *
     * @param id     id
     * @param name   名称
     * @param clazz  类
     * @param script 脚本
     * @param type   类型
     * @param file   脚本存放位置
     */
    public void buildNode(String id, String name, String clazz, String script, String type, String file) {
        //初始化type
        if (StrUtil.isBlank(type)) {
            type = NodeTypeEnum.COMMON.getCode();
        }

        //检查nodeType是不是规定的类型
        NodeTypeEnum nodeTypeEnum = NodeTypeEnum.getEnumByCode(type);
        if (ObjectUtil.isNull(nodeTypeEnum)) {
            throw new NodeTypeNotSupportException(StrUtil.format("type [{}] is not support", type));
        }

        //进行node的build过程
        LiteFlowNodeBuilder.createNode().setId(id).setName(name).setClazz(clazz).setType(nodeTypeEnum).setScript(script).setFile(file).build();
    }


    /**
     * 构建 chain
     *
     * @param condValueStr        执行规则
     * @param group               分组
     * @param errorResume         是否抛出异常
     * @param any                 满足任意条件，执行完成
     * @param threadExecutorClass 指定线程池
     * @param conditionType       chain 类型
     * @param chainBuilder        chainBuilder
     */
    public void buildChain(String condValueStr
            , String group
            , String errorResume
            , String any
            , String threadExecutorClass
            , ConditionTypeEnum conditionType
            , LiteFlowChainBuilder chainBuilder) {
        if (ObjectUtil.isNull(conditionType)) {
            throw new NotSupportConditionException("ConditionType is not supported");
        }

        if (StrUtil.isBlank(condValueStr)) {
            throw new EmptyConditionValueException("Condition value cannot be empty");
        }

        //如果是when类型的话，有特殊化参数要设置，只针对于when的
        if (conditionType.equals(ConditionTypeEnum.TYPE_WHEN)) {
            chainBuilder.setCondition(
                    LiteFlowConditionBuilder.createWhenCondition()
                            .setErrorResume(errorResume)
                            .setGroup(group)
                            .setAny(any)
                            .setThreadExecutorClass(threadExecutorClass)
                            .setValue(condValueStr)
                            .build()
            ).build();
        } else {
            chainBuilder.setCondition(
                    LiteFlowConditionBuilder.createCondition(conditionType)
                            .setValue(condValueStr)
                            .build()
            ).build();
        }
    }
}
