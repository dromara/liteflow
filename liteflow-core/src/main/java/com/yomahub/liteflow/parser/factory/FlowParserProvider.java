package com.yomahub.liteflow.parser.factory;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ErrorSupportPathException;
import com.yomahub.liteflow.parser.ClassJsonFlowParser;
import com.yomahub.liteflow.parser.ClassXmlFlowParser;
import com.yomahub.liteflow.parser.ClassYmlFlowParser;
import com.yomahub.liteflow.parser.base.FlowParser;
import com.yomahub.liteflow.parser.el.ClassJsonFlowELParser;
import com.yomahub.liteflow.parser.el.ClassXmlFlowELParser;
import com.yomahub.liteflow.parser.el.ClassYmlFlowELParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.yomahub.liteflow.enums.FlowParserTypeEnum.*;

/**
 * 解析器提供者
 * <p>
 *
 * @author junjun
 */
public class FlowParserProvider {

    private static final Logger LOG = LoggerFactory.getLogger(FlowExecutor.class);

    private static final String LOCAL_XML_CONFIG_REGEX = "^[\\w\\:\\-\\@\\/\\\\\\*]+\\.xml$";
    private static final String LOCAL_JSON_CONFIG_REGEX = "^[\\w\\:\\-\\@\\/\\\\\\*]+\\.json$";
    private static final String LOCAL_YML_CONFIG_REGEX = "^[\\w\\:\\-\\@\\/\\\\\\*]+\\.yml$";

    private static final String LOCAL_EL_XML_CONFIG_REGEX = "^[\\w\\:\\-\\@\\/\\\\\\*]+\\.el\\.xml$";

    private static final String LOCAL_EL_JSON_CONFIG_REGEX = "^[\\w\\:\\-\\@\\/\\\\\\*]+\\.el\\.json$";

    private static final String LOCAL_EL_YML_CONFIG_REGEX = "^[\\w\\:\\-\\@\\/\\\\\\*]+\\.el\\.yml$";

    private static final String FORMAT_EL_XML_CONFIG_REGEX = "el_xml:.+";

    private static final String FORMAT_EL_JSON_CONFIG_REGEX = "el_json:.+";

    private static final String FORMAT_EL_YML_CONFIG_REGEX = "el_yml:.+";

    private static final String FORMAT_XML_CONFIG_REGEX = "xml:.+";

    private static final String FORMAT_JSON_CONFIG_REGEX = "json:.+";

    private static final String FORMAT_YML_CONFIG_REGEX = "yml:.+";

    private static final String PREFIX_FORMAT_CONFIG_REGEX = "xml:|json:|yml:|el_xml:|el_json:|el_yml:";

    private static final String CLASS_CONFIG_REGEX = "^(xml:|json:|yml:|el_xml:|el_json:|el_yml:)?\\w+(\\.\\w+)*$";

    private static final String ZK_CONFIG_REGEX = "(xml:|json:|yml:|el_xml:|el_json:|el_yml:)?[\\w\\d][\\w\\d\\.]+\\:(\\d)+(\\,[\\w\\d][\\w\\d\\.]+\\:(\\d)+)*";

    /**
     * 根据配置的地址找到对应的解析器
     */
    public static FlowParser lookup(String path) throws Exception {
        if (isLocalConfig(path)) {
            FlowParserFactory factory = new LocalParserFactory();
            if (ReUtil.isMatch(LOCAL_XML_CONFIG_REGEX, path)) {
                LOG.info("flow info loaded from local file,path={},format type={}", path, TYPE_XML.getType());
                return factory.createXmlParser(path);
            }
            else if (ReUtil.isMatch(LOCAL_JSON_CONFIG_REGEX, path)) {
                LOG.info("flow info loaded from local file,path={},format type={}", path, TYPE_JSON.getType());
                return factory.createJsonParser(path);
            }
            else if (ReUtil.isMatch(LOCAL_YML_CONFIG_REGEX, path)) {
                LOG.info("flow info loaded from local file,path={},format type={}", path, TYPE_YML.getType());
                return factory.createYmlParser(path);
            }
            else if (ReUtil.isMatch(LOCAL_EL_XML_CONFIG_REGEX, path)) {
                LOG.info("flow info loaded from local EL file,path={},format type={}", path, TYPE_EL_XML.getType());
                return factory.createXmlELParser(path);
            }
            else if (ReUtil.isMatch(LOCAL_EL_JSON_CONFIG_REGEX, path)) {
                LOG.info("flow info loaded from local EL file,path={},format type={}", path, TYPE_EL_JSON.getType());
                return factory.createJsonELParser(path);
            }
            else if (ReUtil.isMatch(LOCAL_EL_YML_CONFIG_REGEX, path)) {
                LOG.info("flow info loaded from local EL file,path={},format type={}", path, TYPE_EL_YML.getType());
                return factory.createYmlELParser(path);
            }
        }
        else if (isClassConfig(path)) {
            // 获取最终的className，因为有些可能className前面带了文件类型的标识，比如json:x.x.x.x
            String className = ReUtil.replaceAll(path, PREFIX_FORMAT_CONFIG_REGEX, "");
            FlowParserFactory factory = new ClassParserFactory();
            Class<?> clazz = Class.forName(className);
            if (ClassXmlFlowParser.class.isAssignableFrom(clazz)) {
                LOG.info("flow info loaded from class config,class={},format type={}", className, TYPE_XML.getType());
                return factory.createXmlParser(className);
            }
            else if (ClassJsonFlowParser.class.isAssignableFrom(clazz)) {
                LOG.info("flow info loaded from class config,class={},format type={}", className, TYPE_JSON.getType());
                return factory.createJsonParser(className);
            }
            else if (ClassYmlFlowParser.class.isAssignableFrom(clazz)) {
                LOG.info("flow info loaded from class config,class={},format type={}", className, TYPE_YML.getType());
                return factory.createYmlParser(className);
            }
            else if (ClassXmlFlowELParser.class.isAssignableFrom(clazz)) {
                LOG.info("flow info loaded from class config with el,class={},format type={}", className, TYPE_EL_XML.getType());
                return factory.createXmlELParser(className);
            }
            else if (ClassJsonFlowELParser.class.isAssignableFrom(clazz)) {
                LOG.info("flow info loaded from class config with el,class={},format type={}", className, TYPE_EL_JSON.getType());
                return factory.createJsonELParser(className);
            }
            else if (ClassYmlFlowELParser.class.isAssignableFrom(clazz)) {
                LOG.info("flow info loaded from class config with el,class={},format type={}", className, TYPE_EL_YML.getType());
                return factory.createYmlELParser(className);
            }
            // 自定义类必须实现以上实现类，否则报错
            String errorMsg = StrUtil.format("can't support the format {}", path);
            throw new ErrorSupportPathException(errorMsg);
        }
        else if (isZKConfig(path)) {
            FlowParserFactory factory = new ZookeeperParserFactory();
            if (ReUtil.isMatch(FORMAT_XML_CONFIG_REGEX, path)) {
                LOG.info("flow info loaded from Zookeeper,zkNode={},format type={}", path, TYPE_XML.getType());
                return factory.createXmlParser(path);
            }
            else if (ReUtil.isMatch(FORMAT_JSON_CONFIG_REGEX, path)) {
                LOG.info("flow info loaded from Zookeeper,zkNode={},format type={}", path, TYPE_JSON.getType());
                return factory.createJsonParser(path);
            }
            else if (ReUtil.isMatch(FORMAT_YML_CONFIG_REGEX, path)) {
                LOG.info("flow info loaded from Zookeeper,zkNode={},format type={}", path, TYPE_YML.getType());
                return factory.createYmlParser(path);
            }
            else if (ReUtil.isMatch(FORMAT_EL_XML_CONFIG_REGEX, path)) {
                LOG.info("flow info loaded from Zookeeper with el,zkNode={},format type={}", path, TYPE_EL_XML.getType());
                return factory.createXmlELParser(path);
            }
            else if (ReUtil.isMatch(FORMAT_EL_YML_CONFIG_REGEX, path)) {
                LOG.info("flow info loaded from Zookeeper with el,zkNode={},format type={}", path, TYPE_EL_YML.getType());
                return factory.createYmlELParser(path);
            }
            else if (ReUtil.isMatch(FORMAT_EL_JSON_CONFIG_REGEX, path)) {
                LOG.info("flow info loaded from Zookeeper with el,zkNode={},format type={}", path, TYPE_EL_JSON.getType());
                return factory.createJsonELParser(path);
            }

        }

        // not found
        String errorMsg = StrUtil.format("can't find the parser for path:{}", path);
        throw new ErrorSupportPathException(errorMsg);
    }

    /**
     * 判定是否为本地文件
     */
    private static boolean isLocalConfig(String path) {
        return ReUtil.isMatch(LOCAL_XML_CONFIG_REGEX, path)
                || ReUtil.isMatch(LOCAL_JSON_CONFIG_REGEX, path)
                || ReUtil.isMatch(LOCAL_YML_CONFIG_REGEX, path)
                || ReUtil.isMatch(LOCAL_EL_XML_CONFIG_REGEX, path)
                || ReUtil.isMatch(LOCAL_EL_JSON_CONFIG_REGEX, path)
                || ReUtil.isMatch(LOCAL_EL_YML_CONFIG_REGEX, path);
    }

    /**
     * 判定是否为自定义class配置
     */
    private static boolean isClassConfig(String path) {
        return ReUtil.isMatch(CLASS_CONFIG_REGEX, path);
    }

    /**
     * 判定是否为zk配置
     */
    private static boolean isZKConfig(String path) {
        return ReUtil.isMatch(ZK_CONFIG_REGEX, path);
    }
}
