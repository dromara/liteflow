package com.yomahub.liteflow.parser.factory;

import com.yomahub.liteflow.parser.base.BaseJsonFlowParser;
import com.yomahub.liteflow.parser.base.BaseXmlFlowParser;
import com.yomahub.liteflow.parser.base.BaseYmlFlowParser;

/**
 * Flow Parser 工厂接口
 * <p>
 *
 * @author junjun
 */
public interface FlowParserFactory {

    BaseJsonFlowParser createJsonParser(String path);

    BaseXmlFlowParser createXmlParser(String path);

    BaseYmlFlowParser createYmlParser(String path);

    BaseJsonFlowParser createJsonELParser(String path);

    BaseXmlFlowParser createXmlELParser(String path);

    BaseYmlFlowParser createYmlELParser(String path);

}
