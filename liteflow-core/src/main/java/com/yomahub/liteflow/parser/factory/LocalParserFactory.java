package com.yomahub.liteflow.parser.factory;

import com.yomahub.liteflow.parser.*;
import com.yomahub.liteflow.parser.el.*;

/**
 * 本地文件
 * <p>
 *
 * @author junjun
 */
public class LocalParserFactory implements FlowParserFactory {

    @Override
    public JsonFlowParser createJsonParser(String path) {
        return new LocalJsonFlowParser();
    }

    @Override
    public XmlFlowParser createXmlParser(String path) {
        return new LocalXmlFlowParser();
    }

    @Override
    public YmlFlowParser createYmlParser(String path) {
        return new LocalYmlFlowParser();
    }

    @Override
    public JsonFlowELParser createJsonELParser(String path) {
        return new LocalJsonFlowELParser();
    }

    @Override
    public XmlFlowELParser createXmlELParser(String path) {
        return new LocalXmlFlowELParser();
    }

    @Override
    public YmlFlowELParser createYmlELParser(String path) {
        return new LocalYmlFlowELParser();
    }
}
