package com.yomahub.liteflow.parser.spi.zk;

import com.yomahub.liteflow.parser.spi.ParserClassNameSpi;
import com.yomahub.liteflow.parser.zk.ZkXmlELParser;

/**
 * ZK解析器SPI实现
 *
 * @author Bryan.Zhang
 * @since 2.8.6
 */
public class ZkParserClassNameSpi implements ParserClassNameSpi {

	@Override
	public String getSpiClassName() {
		return ZkXmlELParser.class.getName();
	}

}
