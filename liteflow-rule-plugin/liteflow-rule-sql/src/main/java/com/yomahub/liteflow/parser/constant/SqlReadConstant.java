package com.yomahub.liteflow.parser.constant;

/**
 * Copyright (C), 2021, 北京同创永益科技发展有限公司
 *
 * @author tangkc
 * @version 3.0.0
 * @description
 * @date 2023/9/28 11:42
 */
public class SqlReadConstant {

    public static final String SQL_PATTERN = "SELECT {},{} FROM {} WHERE {}=?";

    public static final String SCRIPT_SQL_CHECK_PATTERN = "SELECT 1 FROM {} ";

    public static final String SCRIPT_SQL_PATTERN = "SELECT {},{},{},{} FROM {} WHERE {}=?";

    public static final String SCRIPT_WITH_LANGUAGE_SQL_PATTERN = "SELECT {},{},{},{},{} FROM {} WHERE {}=?";

    public static final String CHAIN_XML_PATTERN = "<chain name=\"{}\"><![CDATA[{}]]></chain>";

    public static final String NODE_XML_PATTERN = "<nodes>{}</nodes>";

    public static final String NODE_ITEM_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\"><![CDATA[{}]]></node>";

    public static final String NODE_ITEM_WITH_LANGUAGE_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\" language=\"{}\"><![CDATA[{}]]></node>";

    public static final String XML_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow>{}{}</flow>";

    public static final Integer FETCH_SIZE_MAX = 1000;
}
