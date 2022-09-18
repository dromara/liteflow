package com.yomahub.liteflow.parser.spi.sql;

import com.yomahub.liteflow.parser.spi.ParserClassNameSpi;
import com.yomahub.liteflow.parser.sql.SQLXmlELParser;

/**
 * SQL 解析器 SPI 实现
 *
 * @author tangkc
 * @since 2.9.0
 */
public class SQLParserClassNameSpi implements ParserClassNameSpi {

	@Override
	public String getSpiClassName() {
		return SQLXmlELParser.class.getName();
	}

}