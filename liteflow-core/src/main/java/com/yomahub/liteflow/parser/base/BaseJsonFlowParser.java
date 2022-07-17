package com.yomahub.liteflow.parser.base;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.yomahub.liteflow.parser.helper.ParserHelper;

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

		List<JSONObject> jsonObjectList = ListUtil.toList();
		for (String content : contentList) {
			//把字符串原生转换为json对象，如果不加第二个参数OrderedField，会无序
			JSONObject flowJsonObject = JSONObject.parseObject(content, Feature.OrderedField);
			jsonObjectList.add(flowJsonObject);
		}
		ParserHelper.parseJsonObject(jsonObjectList, CHAIN_NAME_SET, this::parseOneChain);
	}

	/**
	 * 解析一个chain的过程
	 *
	 * @param chainObject chain 节点
	 */
	public abstract void parseOneChain(JSONObject chainObject);

}
