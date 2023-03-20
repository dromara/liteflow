package com.yomahub.liteflow.parser.base;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.parser.helper.ParserHelper;
import com.yomahub.liteflow.util.JsonUtil;
import org.yaml.snakeyaml.Yaml;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 基类，用于存放 YmlFlowParser 通用方法
 *
 * @author tangkc
 */
public abstract class BaseYmlFlowParser implements FlowParser {

	private final Set<String> CHAIN_NAME_SET = new HashSet<>();

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
			JsonNode ruleObject = convertToJson(content);
			jsonObjectList.add(ruleObject);
		}

		Consumer<JsonNode> parseOneChainConsumer = this::parseOneChain;
		ParserHelper.parseNodeJson(jsonObjectList);
		ParserHelper.parseChainJson(jsonObjectList, CHAIN_NAME_SET, parseOneChainConsumer);
	}

	protected JsonNode convertToJson(String yamlString) {
		Yaml yaml = new Yaml();
		Map<String, Object> map = yaml.load(yamlString);
		return JsonUtil.parseObject(JsonUtil.toJsonString(map));
	}

	/**
	 * 解析一个 chain 的过程
	 * @param chain chain
	 */
	public abstract void parseOneChain(JsonNode chain);

}
