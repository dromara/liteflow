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

/**
 * Flow Parser 工厂接口
 * <p>
 *
 * @author junjun
 */
public interface FlowParserFactory {

    BaseJsonFlowParser createJsonParser(String path) throws Exception;

    BaseXmlFlowParser createXmlParser(String path) throws Exception;

    BaseYmlFlowParser createYmlParser(String path) throws Exception;

    BaseJsonFlowParser createJsonELParser(String path) throws Exception;

    BaseXmlFlowParser createXmlELParser(String path) throws Exception;

    BaseYmlFlowParser createYmlELParser(String path) throws Exception;


}
