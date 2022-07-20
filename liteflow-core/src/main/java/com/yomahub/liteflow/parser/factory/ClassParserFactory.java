package com.yomahub.liteflow.parser.factory;

import com.yomahub.liteflow.parser.JsonFlowParser;
import com.yomahub.liteflow.parser.XmlFlowParser;
import com.yomahub.liteflow.parser.YmlFlowParser;
import com.yomahub.liteflow.parser.base.BaseJsonFlowParser;
import com.yomahub.liteflow.parser.base.BaseXmlFlowParser;
import com.yomahub.liteflow.parser.base.BaseYmlFlowParser;
import com.yomahub.liteflow.parser.el.JsonFlowELParser;
import com.yomahub.liteflow.parser.el.XmlFlowELParser;
import com.yomahub.liteflow.parser.el.YmlFlowELParser;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;

/**
 * Class文件
 * <p>
 *
 * @author junjun
 */
public class ClassParserFactory implements FlowParserFactory {

    @Override
    public JsonFlowParser createJsonParser(String path) throws Exception {
        Class<?> c = Class.forName(path);
        return (JsonFlowParser) ContextAwareHolder.loadContextAware().registerBean(c);
    }

    @Override
    public XmlFlowParser createXmlParser(String path) throws Exception {
        Class<?> c = Class.forName(path);
        return (XmlFlowParser) ContextAwareHolder.loadContextAware().registerBean(c);
    }

    @Override
    public YmlFlowParser createYmlParser(String path) throws Exception {
        Class<?> c = Class.forName(path);
        return (YmlFlowParser) ContextAwareHolder.loadContextAware().registerBean(c);
    }

    @Override
    public BaseJsonFlowParser createJsonELParser(String path) throws Exception {
        Class<?> c = Class.forName(path);
        return (JsonFlowELParser) ContextAwareHolder.loadContextAware().registerBean(c);
    }

    @Override
    public BaseXmlFlowParser createXmlELParser(String path) throws Exception {
        Class<?> c = Class.forName(path);
        return (XmlFlowELParser) ContextAwareHolder.loadContextAware().registerBean(c);
    }

    @Override
    public BaseYmlFlowParser createYmlELParser(String path) throws Exception {
        Class<?> c = Class.forName(path);
        return (YmlFlowELParser) ContextAwareHolder.loadContextAware().registerBean(c);
    }
}
