package com.yomahub.liteflow.parser.factory;

import com.yomahub.liteflow.parser.*;
import com.yomahub.liteflow.parser.base.BaseJsonFlowParser;
import com.yomahub.liteflow.parser.base.BaseXmlFlowParser;
import com.yomahub.liteflow.parser.base.BaseYmlFlowParser;
import com.yomahub.liteflow.parser.el.*;
import com.yomahub.liteflow.property.LiteflowConfigGetter;

/**
 * Class文件
 * <p>
 *
 * @author junjun
 */
public class ZookeeperParserFactory implements FlowParserFactory {
    
    @Override
    public BaseJsonFlowParser createJsonParser(String path) {
        return new ZookeeperJsonFlowParser(LiteflowConfigGetter.get().getZkNode());
    }

    @Override
    public BaseXmlFlowParser createXmlParser(String path) {
        return new ZookeeperXmlFlowParser(LiteflowConfigGetter.get().getZkNode());
    }

    @Override
    public BaseYmlFlowParser createYmlParser(String path) {
        return new ZookeeperYmlFlowParser(LiteflowConfigGetter.get().getZkNode());
    }

    @Override
    public BaseJsonFlowParser createJsonELParser(String path) {
        return new ZookeeperJsonFlowELParser(LiteflowConfigGetter.get().getZkNode());
    }

    @Override
    public BaseXmlFlowParser createXmlELParser(String path) {
        return new ZookeeperXmlFlowELParser(LiteflowConfigGetter.get().getZkNode());
    }

    @Override
    public BaseYmlFlowParser createYmlELParser(String path) {
        return new ZookeeperYmlFlowELParser(LiteflowConfigGetter.get().getZkNode());
    }
}
