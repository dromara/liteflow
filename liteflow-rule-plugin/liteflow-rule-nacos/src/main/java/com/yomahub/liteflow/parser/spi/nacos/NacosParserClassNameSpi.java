package com.yomahub.liteflow.parser.spi.nacos;

import com.yomahub.liteflow.parser.nacos.NacosXmlELParser;
import com.yomahub.liteflow.parser.spi.ParserClassNameSpi;

/**
 * Nacos 解析器SPI实现
 *
 * @author mll
 * @since 2.9.0
 */
public class NacosParserClassNameSpi implements ParserClassNameSpi {

	@Override
	public String getSpiClassName() {
		return NacosXmlELParser.class.getName();
	}

}
