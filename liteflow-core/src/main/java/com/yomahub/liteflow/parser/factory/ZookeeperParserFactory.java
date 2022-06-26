package com.yomahub.liteflow.parser.factory;

import com.yomahub.liteflow.parser.*;
import com.yomahub.liteflow.property.LiteflowConfigGetter;

/**
 * Class文件
 * <p>
 *
 * @author junjun
 */
public class ZookeeperParserFactory implements FlowParserFactory {
    
    @Override
    public JsonFlowParser createJsonParser(String path) {
        return new ZookeeperJsonFlowParser(LiteflowConfigGetter.get().getZkNode());
    }

    @Override
    public XmlFlowParser createXmlParser(String path) {
        return new ZookeeperXmlFlowParser(LiteflowConfigGetter.get().getZkNode());
    }

    @Override
    public YmlFlowParser createYmlParser(String path) {
        return new ZookeeperYmlFlowParser(LiteflowConfigGetter.get().getZkNode());
    }
}
