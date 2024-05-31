package com.yomahub.liteflow.parser.constant;

/**
 * sql 读取常量类
 *
 * @author tangkc
 * @author houxinyu
 * @author Bryan.Zhang
 * @since 2.11.1
 */
public class SqlReadConstant {

    public static final String SQL_PATTERN = "SELECT * FROM {} WHERE {}=?";

    public static final String SCRIPT_SQL_CHECK_PATTERN = "SELECT 1 FROM {} ";

    public static final String SCRIPT_SQL_PATTERN = "SELECT * FROM {} WHERE {}=?";

    public static final String CHAIN_XML_PATTERN = "<chain id=\"{}\" namespace=\"{}\"><route><![CDATA[{}]]></route><body><![CDATA[{}]]></body></chain>";

    public static final String NODE_XML_PATTERN = "<nodes>{}</nodes>";

    public static final String NODE_ITEM_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\"><![CDATA[{}]]></node>";

    public static final String NODE_ITEM_WITH_LANGUAGE_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\" language=\"{}\"><![CDATA[{}]]></node>";

    public static final String XML_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow>{}{}</flow>";

    public static final Integer FETCH_SIZE_MAX = 1000;
}
