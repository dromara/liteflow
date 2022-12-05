package com.yomahub.liteflow.parser.spi.apollo;

import com.yomahub.liteflow.parser.apollo.ApolloXmlELParser;
import com.yomahub.liteflow.parser.spi.ParserClassNameSpi;

/**
 * @Description:
 * @Author: zhanghua
 * @Date: 2022/12/3 13:40
 */
public class ApolloParserClassNameSpi implements ParserClassNameSpi {

	@Override
	public String getSpiClassName() {
		return ApolloXmlELParser.class.getName();
	}
}
