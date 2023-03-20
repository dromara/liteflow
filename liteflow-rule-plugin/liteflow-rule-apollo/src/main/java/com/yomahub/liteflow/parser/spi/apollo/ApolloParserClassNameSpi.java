package com.yomahub.liteflow.parser.spi.apollo;

import com.yomahub.liteflow.parser.apollo.ApolloXmlELParser;
import com.yomahub.liteflow.parser.spi.ParserClassNameSpi;

/**
 * @author zhanghua
 * @since 2.9.5
 */
public class ApolloParserClassNameSpi implements ParserClassNameSpi {

	@Override
	public String getSpiClassName() {
		return ApolloXmlELParser.class.getName();
	}

}
