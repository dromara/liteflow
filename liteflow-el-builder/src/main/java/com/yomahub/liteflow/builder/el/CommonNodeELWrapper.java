package com.yomahub.liteflow.builder.el;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.util.JsonUtil;

import java.util.Map;

/**
 * 普通节点表示
 *
 * @author gezuao
 * @author luo yi
 * @since 2.12.3
 */
public class CommonNodeELWrapper extends ELWrapper {

    private String nodeId;

    private String tag;

    public CommonNodeELWrapper(String nodeId) {
        this.nodeId = nodeId;
        this.setNodeWrapper(this);
    }

    private void setNodeWrapper(ELWrapper elWrapper){
        this.addWrapper(elWrapper, 0);
    }

    private CommonNodeELWrapper getNodeWrapper(){
        return (CommonNodeELWrapper) this.getFirstWrapper();
    }

    protected String getNodeId() {
        return nodeId;
    }

    protected void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public CommonNodeELWrapper tag(String tag) {
        this.setTag(tag);
        return this;
    }

    /**
     * 单节点不允许定义 id，重载为protected修饰
     *
     * @param id 节点id
     * @return {@link CommonNodeELWrapper}
     */
    @Override
    public CommonNodeELWrapper id(String id) {
        this.setId(id);
        return this;
    }

    @Override
    public CommonNodeELWrapper data(String dataName, Object object) {
        setData("'" + JsonUtil.toJsonString(object) + "'");
        setDataName(dataName);
        return this;
    }

    @Override
    public CommonNodeELWrapper data(String dataName, String jsonString) {
        setData("'" + jsonString + "'");
        setDataName(dataName);
        return this;
    }

    @Override
    public CommonNodeELWrapper data(String dataName, Map<String, Object> jsonMap) {
        setData("'" + JsonUtil.toJsonString(jsonMap) + "'");
        setDataName(dataName);
        return this;
    }

    @Override
    public CommonNodeELWrapper maxWaitSeconds(Integer maxWaitSeconds){
        setMaxWaitSeconds(maxWaitSeconds);
        return this;
    }

    public CommonNodeELWrapper retry(Integer count){
        super.retry(count);
        return this;
    }

    public CommonNodeELWrapper retry(Integer count, String... exceptions){
        super.retry(count, exceptions);
        return this;
    }

    @Override
    protected String toEL(Integer depth, StringBuilder paramContext) {
        CommonNodeELWrapper commonNodeElWrapper = this.getNodeWrapper();
        StringBuilder sb = new StringBuilder();
        processWrapperTabs(sb, depth);
        sb.append(commonNodeElWrapper.getNodeId());
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
