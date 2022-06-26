package com.yomahub.liteflow.parser.factory;

import com.yomahub.liteflow.parser.JsonFlowParser;
import com.yomahub.liteflow.parser.XmlFlowParser;
import com.yomahub.liteflow.parser.YmlFlowParser;

/**
 * Flow Parser 工厂接口
 * <p>
 *
 * @author junjun
 */
public interface FlowParserFactory {

    JsonFlowParser createJsonParser(String path) throws Exception;

    XmlFlowParser createXmlParser(String path) throws Exception;

    YmlFlowParser createYmlParser(String path) throws Exception;

}
