package com.yomahub.liteflow.parser.constant;

/**
 * sql 读取常量类
 *
 * @author tangkc
 * @author houxinyu
 * @author Bryan.Zhang
 * @author jay li
 * @since 2.11.1
 */
public class SqlReadConstant {
    public static final String INSTANT_SELECT_SQL = "SELECT count(*) FROM {} where {} = '{}' and {} = '{}' ";

    public static final String INSTANT_UPDATE_SQL = "UPDATE {} SET {} = '{}',{} = '{}' WHERE {} = '{}' and {} = '{}'";

    public static final String INSTANT_INSERT_SQL = "INSERT INTO {} ({},{},{},{}) VALUES ('{}','{}','{}','{}')";

    public static final String INSTANT_CREATE_TABLE_SQL = "create table IF NOT EXISTS node_instance_id_table\n" +
            "(\n" +
            "    application_name     varchar(32)   NOT NULL,\n" +
            "    chain_id           varchar(32)   NOT NULL,\n" +
            "    el_data_md5          varchar(128)   NOT NULL,\n" +
            "    node_instance_id_map_json     varchar(1024)   NOT NULL\n" +
            ");";

    public static final String SQL_PATTERN = "SELECT * FROM {} WHERE {}='{}'";

    public static final String SQL_PATTERN_WITH_CHAIN_ID = "SELECT * FROM {} WHERE {}='{}' and  {}='{}'";

    public static final String SCRIPT_SQL_CHECK_PATTERN = "SELECT 1 FROM {} ";

    public static final String SCRIPT_SQL_PATTERN = "SELECT * FROM {} WHERE {}='{}'";

    public static final String CHAIN_XML_PATTERN = "<chain id=\"{}\" namespace=\"{}\"><route><![CDATA[{}]]></route><body><![CDATA[{}]]></body></chain>";

    public static final String NODE_XML_PATTERN = "<nodes>{}</nodes>";

    public static final String NODE_ITEM_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\"><![CDATA[{}]]></node>";

    public static final String NODE_ITEM_WITH_LANGUAGE_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\" language=\"{}\"><![CDATA[{}]]></node>";

    public static final String XML_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow>{}{}</flow>";

    public static final Integer FETCH_SIZE_MAX = 1000;
}
