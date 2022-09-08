package com.yomahub.liteflow.enums;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.core.NodeIfComponent;
import com.yomahub.liteflow.core.NodeSwitchComponent;
import jdk.nashorn.internal.objects.annotations.Getter;

/**
 * 注解节点类型枚举
 *
 * @author Sorghum
 * @date 2022/09/08
 */
public enum AnnotionNodeTypeEnum {
    /**
     * 普通节点
     */
    COMMON("普通", NodeComponent.class),
    /**
     * 选择节点
     */
    SWITCH("选择", NodeSwitchComponent.class),
    /**
     * 条件节点
     */
    IF("条件", NodeIfComponent.class),;
    /**
     * 描述
     */
    final String desc;
    /**
     * cmp类
     */
    final Class<? extends NodeComponent> cmpClass;

    AnnotionNodeTypeEnum(String desc, Class<? extends NodeComponent> cmpClass) {
        this.desc = desc;
        this.cmpClass = cmpClass;
    }

    /**
     * 得到Node定义类
     *
     * @return {@link Class}<{@link ?} {@link extends} {@link NodeComponent}>
     */
    public Class<? extends NodeComponent> getCmpClass() {
        return cmpClass;
    }
}
