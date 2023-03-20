package com.yomahub.liteflow.parser.base;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.parser.helper.ParserHelper;
import com.yomahub.liteflow.util.JsonUtil;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 基类，用于存放 JsonFlowParser 通用方法
 *
 * @author tangkc
 */
public abstract class BaseJsonFlowParser implements FlowParser {

	private final Set<String> CHAIN_NAME_SET = new CopyOnWriteArraySet<>();

	public void parse(String content) throws Exception {
		parse(ListUtil.toList(content));
	}

	@Override
	public void parse(List<String> contentList) throws Exception {
		if (CollectionUtil.isEmpty(contentList)) {
			return;
		}

		List<JsonNode> jsonObjectList = ListUtil.toList();
		for (String content : contentList) {
			JsonNode flowJsonNode = JsonUtil.parseObject(content);
			jsonObjectList.add(flowJsonNode);
		}
		ParserHelper.parseNodeJson(jsonObjectList);
		ParserHelper.parseChainJson(jsonObjectList, CHAIN_NAME_SET, this::parseOneChain);
	}

	/**
	 * 解析一个chain的过程
	 * @param chainObject chain 节点
	 */
	public abstract void parseOneChain(JsonNode chainObject);

}
