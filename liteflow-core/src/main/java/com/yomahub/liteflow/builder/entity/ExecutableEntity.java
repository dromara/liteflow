package com.yomahub.liteflow.builder.entity;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 *  执行器的实体类
 * </pre>
 *
 * @author sikadai
 * @version 2.6.11
 * @since 2022/3/13 15:28
 */
public class ExecutableEntity {
    private String id;
    private String tag;
    private List<ExecutableEntity> nodeCondComponents;

    public ExecutableEntity() {

    }

    public ExecutableEntity(String id, String tag) {
        this.id = id;
        this.tag = tag;
    }

    public String getId() {
        return id;
    }

    public ExecutableEntity setId(String id) {
        this.id = id;
        return this;
    }

    public String getTag() {
        return tag;
    }

    public ExecutableEntity setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public List<ExecutableEntity> getNodeCondComponents() {
        return nodeCondComponents;
    }

    public ExecutableEntity setNodeCondComponents(List<ExecutableEntity> nodeCondComponents) {
        this.nodeCondComponents = nodeCondComponents;
        return this;
    }

    public ExecutableEntity addNodeCondComponent(ExecutableEntity conditionNodeEntity) {
        if (CollUtil.isEmpty(this.nodeCondComponents)) {
            this.nodeCondComponents = new ArrayList<>();
        }
        this.nodeCondComponents.add(conditionNodeEntity);
        return this;
    }
}
