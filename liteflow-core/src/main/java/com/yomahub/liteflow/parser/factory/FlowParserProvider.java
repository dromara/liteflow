package com.yomahub.liteflow.parser.factory;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.exception.ErrorSupportPathException;
import com.yomahub.liteflow.parser.ClassJsonFlowParser;
import com.yomahub.liteflow.parser.ClassXmlFlowParser;
import com.yomahub.liteflow.parser.ClassYmlFlowParser;
import com.yomahub.liteflow.parser.FlowParser;
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

    private static final String FORMATE_XML_CONFIG_REGEX = "xml:.+";
    private static final String FORMATE_JSON_CONFIG_REGEX = "json:.+";
    private static final String FORMATE_YML_CONFIG_REGEX = "yml:.+";

    private static final String CLASS_CONFIG_REGEX = "^\\w+(\\.\\w+)*$";

    /**
     * 根据配置的地址找到对应的解析器
     *
     * @param path
     * @return
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
        }
        else if (isClassConfig(path)) {
            FlowParserFactory factory = new ClassParserFactory();
            Class<?> clazz = Class.forName(path);
            if (ClassXmlFlowParser.class.isAssignableFrom(clazz)) {
                LOG.info("flow info loaded from class config,class={},format type={}", path, TYPE_XML.getType());
                return factory.createXmlParser(path);
            }
            else if (ClassJsonFlowParser.class.isAssignableFrom(clazz)) {
                LOG.info("flow info loaded from class config,class={},format type={}", path, TYPE_JSON.getType());
                return factory.createJsonParser(path);
            }
            else if (ClassYmlFlowParser.class.isAssignableFrom(clazz)) {
                LOG.info("flow info loaded from class config,class={},format type={}", path, TYPE_YML.getType());
                return factory.createYmlParser(path);
            }
            // 自定义类必须实现以上实现类，否则报错
            String errorMsg = StrUtil.format("can't support the format {}", path);
            throw new ErrorSupportPathException(errorMsg);
        }
        else if (isZKConfig(path)) {
            FlowParserFactory factory = new ZookeeperParserFactory();
            if (ReUtil.isMatch(FORMATE_XML_CONFIG_REGEX, path)) {
                LOG.info("flow info loaded from Zookeeper,zkNode={},format type={}", path, TYPE_XML.getType());
                return factory.createXmlParser(path);
            }
            else if (ReUtil.isMatch(FORMATE_JSON_CONFIG_REGEX, path)) {
                LOG.info("flow info loaded from Zookeeper,zkNode={},format type={}", path, TYPE_JSON.getType());
                return factory.createJsonParser(path);
            }
            else if (ReUtil.isMatch(FORMATE_YML_CONFIG_REGEX, path)) {
                LOG.info("flow info loaded from Zookeeper,zkNode={},format type={}", path, TYPE_YML.getType());
                return factory.createYmlParser(path);
            }
        }

        // not found
        throw new ConfigErrorException("parse error, please check liteflow config property");
    }

    /**
     * 判定是否为本地文件
     */
    private static boolean isLocalConfig(String path) {
        return ReUtil.isMatch(LOCAL_XML_CONFIG_REGEX, path)
                || ReUtil.isMatch(LOCAL_JSON_CONFIG_REGEX, path)
                || ReUtil.isMatch(LOCAL_YML_CONFIG_REGEX, path);
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
        return ReUtil.isMatch(FORMATE_XML_CONFIG_REGEX, path)
                || ReUtil.isMatch(FORMATE_JSON_CONFIG_REGEX, path)
                || ReUtil.isMatch(FORMATE_YML_CONFIG_REGEX, path);
    }
}
