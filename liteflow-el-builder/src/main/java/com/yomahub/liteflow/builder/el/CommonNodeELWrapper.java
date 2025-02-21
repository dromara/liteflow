package com.yomahub.liteflow.builder.el;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.util.JsonUtil;

import java.util.Map;

/**
 * 普通节点表示
 *
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
        super.data(dataName, object);
        return this;
    }

    @Override
    public CommonNodeELWrapper data(String dataName, String jsonString) {
        super.data(dataName, jsonString);
        return this;
    }

    @Override
    public CommonNodeELWrapper data(String dataName, Map<String, Object> jsonMap) {
        super.data(dataName, jsonMap);
        return this;
    }

    @Override
    public ELWrapper bind(String key, String value) {
        super.bind(key, value);
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
}
