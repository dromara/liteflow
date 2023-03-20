package com.yomahub.liteflow.parser.spi.etcd;

import com.yomahub.liteflow.parser.etcd.EtcdXmlELParser;
import com.yomahub.liteflow.parser.spi.ParserClassNameSpi;

/**
 * Etcd解析器SPI实现
 *
 * @author zendwang
 * @since 2.9.0
 */
public class EtcdParserClassNameSpi implements ParserClassNameSpi {

	@Override
	public String getSpiClassName() {
		return EtcdXmlELParser.class.getName();
	}

}
