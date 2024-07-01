package com.yomahub.liteflow.builder.el;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.util.JsonUtil;

import java.util.Map;

/**
 * 单节点表达式
 * 单节点也应以为一种表达式
 * 支持设置 tag data maxWaitSeconds 属性
 *
 * @author gezuao
 * @since 2.11.1
 */
public class NodeELWrapper extends ELWrapper {

    private String nodeId;

    private String tag;

    public NodeELWrapper(String nodeId) {
        this.nodeId = nodeId;
        this.setNodeWrapper(this);
    }

    private void setNodeWrapper(ELWrapper elWrapper){
        this.addWrapper(elWrapper, 0);
    }

    private NodeELWrapper getNodeWrapper(){
        return (NodeELWrapper) this.getFirstWrapper();
    }

    protected String getNodeId() {
        return nodeId;
    }

    protected void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public NodeELWrapper tag(String tag) {
        this.setTag(tag);
        return this;
    }

    /**
     * 单节点不允许定义 id，重载为protected修饰
     *
     * @param id 节点id
     * @return {@link NodeELWrapper}
     */
    @Override
    public NodeELWrapper id(String id) {
        this.setId(id);
        return this;
    }

    @Override
    public NodeELWrapper data(String dataName, Object object) {
        setData("'" + JsonUtil.toJsonString(object) + "'");
        setDataName(dataName);
        return this;
    }

    @Override
    public NodeELWrapper data(String dataName, String jsonString) {
        setData("'" + jsonString + "'");
        setDataName(dataName);
        return this;
    }

    @Override
    public NodeELWrapper data(String dataName, Map<String, Object> jsonMap) {
        setData("'" + JsonUtil.toJsonString(jsonMap) + "'");
        setDataName(dataName);
        return this;
    }

    @Override
    public NodeELWrapper maxWaitSeconds(Integer maxWaitSeconds){
        setMaxWaitSeconds(maxWaitSeconds);
        return this;
    }

    public NodeELWrapper retry(Integer count){
        super.retry(count);
        return this;
    }

    public NodeELWrapper retry(Integer count, String... exceptions){
        super.retry(count, exceptions);
        return this;
    }

    @Override
    protected String toEL(Integer depth, StringBuilder paramContext) {
        NodeELWrapper nodeElWrapper = this.getNodeWrapper();
        StringBuilder sb = new StringBuilder();
        processWrapperTabs(sb, depth);
        if (ELBus.isNodeWrapper()){
            sb.append(StrUtil.format("node(\"{}\")", nodeElWrapper.getNodeId()));
        }else{
            sb.append(StrUtil.format("{}", nodeElWrapper.getNodeId()));
        }
        processWrapperProperty(sb, paramContext);
        return sb.toString();
    }

    /**
     * Node的公共属性不包括id，对父类方法重载。
     *
     * @param elContext    EL 上下文
     * @param paramContext 参数上下文
     */
    @Override
    protected void processWrapperProperty(StringBuilder elContext, StringBuilder paramContext){
        if(this.getTag() != null){
            elContext.append(StrUtil.format(".tag(\"{}\")", this.getTag()));
        }
        if(this.getData() != null){
            elContext.append(StrUtil.format(".data({})", this.getDataName()));
            paramContext.append(StrUtil.format("{} = {}", this.getDataName(), this.getData())).append(";\n");
        }
        if(this.getMaxWaitSeconds() != null){
            elContext.append(StrUtil.format(".maxWaitSeconds({})", String.valueOf(this.getMaxWaitSeconds())));
        }
        if (this.getRetry() != null){
            elContext.append(StrUtil.format(".retry({})", this.getRetry().toString()));
        }
    }
}
