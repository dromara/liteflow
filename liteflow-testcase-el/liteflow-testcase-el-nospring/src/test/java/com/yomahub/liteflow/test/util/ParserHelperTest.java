package com.yomahub.liteflow.test.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.parser.helper.ParserHelper;
import com.yomahub.liteflow.util.JsonUtil;
import org.junit.Test;

import java.util.Iterator;

import static com.yomahub.liteflow.common.ChainConstant.NAME;
import static com.yomahub.liteflow.common.ChainConstant.VALUE;

public class ParserHelperTest {

	private  final static String FLOW_JSON = "{\"flow\":{\"nodes\":{\"node\":[{\"id\":\"a\",\"class\":\"com.yomahub.liteflow.test.parser.cmp.ACmp\"},{\"id\":\"b\",\"class\":\"com.yomahub.liteflow.test.parser.cmp.BCmp\"},{\"id\":\"c\",\"class\":\"com.yomahub.liteflow.test.parser.cmp.CCmp\"},{\"id\":\"d\",\"class\":\"com.yomahub.liteflow.test.parser.cmp.DCmp\"},{\"id\":\"e\",\"class\":\"com.yomahub.liteflow.test.parser.cmp.ECmp\"},{\"id\":\"f\",\"class\":\"com.yomahub.liteflow.test.parser.cmp.FCmp\"},{\"id\":\"g\",\"class\":\"com.yomahub.liteflow.test.parser.cmp.GCmp\"}]},\"chain\":[{\"name\":\"chain2\",\"value\":\"THEN(c,g,f);\"},{\"name\":\"chain1\",\"value\":\"THEN(a,c,WHEN(b,d,SWITCH(e).to(f,g)), chain2);\"}]}}";


	@Test
	public void testParseOneChainEl4JsonNode() {
		JsonNode rootNode = JsonUtil.parseObject(FLOW_JSON);
		Iterator<JsonNode> iterator = rootNode.path("flow").path("chain").iterator();
		while (iterator.hasNext()) {
			JsonNode chainNode = iterator.next();
			String chainName = chainNode.get(NAME).textValue();
			String el = chainNode.get(VALUE).textValue();
			System.out.println(chainName + "-" + el);
		}

	}
}
