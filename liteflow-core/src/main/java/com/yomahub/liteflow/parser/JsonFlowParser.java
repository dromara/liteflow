package com.yomahub.liteflow.parser;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.yomahub.liteflow.builder.LiteFlowChainBuilder;
import com.yomahub.liteflow.builder.prop.ChainPropBean;
import com.yomahub.liteflow.builder.prop.NodePropBean;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.exception.ChainDuplicateException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.spi.holder.ContextCmpInitHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.yomahub.liteflow.common.ChainConstant.ANY;
import static com.yomahub.liteflow.common.ChainConstant.CHAIN;
import static com.yomahub.liteflow.common.ChainConstant.CONDITION;
import static com.yomahub.liteflow.common.ChainConstant.ERROR_RESUME;
import static com.yomahub.liteflow.common.ChainConstant.FILE;
import static com.yomahub.liteflow.common.ChainConstant.FLOW;
import static com.yomahub.liteflow.common.ChainConstant.GROUP;
import static com.yomahub.liteflow.common.ChainConstant.ID;
import static com.yomahub.liteflow.common.ChainConstant.NAME;
import static com.yomahub.liteflow.common.ChainConstant.NODE;
import static com.yomahub.liteflow.common.ChainConstant.NODES;
import static com.yomahub.liteflow.common.ChainConstant.THREAD_EXECUTOR_CLASS;
import static com.yomahub.liteflow.common.ChainConstant.TYPE;
import static com.yomahub.liteflow.common.ChainConstant.VALUE;
import static com.yomahub.liteflow.common.ChainConstant._CLASS;

/**
 * Json格式解析器
 *
 * @author guodongqing
 * @since 2.5.0
 */
public abstract class JsonFlowParser extends BaseFlowParser {

	private final Logger LOG = LoggerFactory.getLogger(JsonFlowParser.class);

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

		parseJsonObject(jsonObjectList);
	}

	//json格式，解析过程
	public void parseJsonObject(List<JSONObject> flowJsonObjectList) throws Exception {
		//在相应的环境下进行节点的初始化工作
		//在spring体系下会获得spring扫描后的节点，接入元数据
		//在非spring体系下是一个空实现，等于不做此步骤
		ContextCmpInitHolder.loadContextCmpInit().initCmp();

		//先在元数据里放上chain
		//先放有一个好处，可以在parse的时候先映射到FlowBus的chainMap，然后再去解析
		//这样就不用去像之前的版本那样回归调用
		//同时也解决了不能循环依赖的问题
		flowJsonObjectList.forEach(jsonObject -> {
			// 解析chain节点
			JSONArray chainArray = jsonObject.getJSONObject(FLOW).getJSONArray(CHAIN);

			//先在元数据里放上chain
			chainArray.forEach(o -> {
				JSONObject innerJsonObject = (JSONObject) o;
				//校验加载的 chainName 是否有重复的
				//TODO 这里是否有个问题，当混合格式加载的时候，2个同名的Chain在不同的文件里，就不行了
				String chainName = innerJsonObject.getString(NAME);
				if (!CHAIN_NAME_SET.add(chainName)) {
					throw new ChainDuplicateException(String.format("[chain name duplicate] chainName=%s", chainName));
				}

				FlowBus.addChain(innerJsonObject.getString(NAME));
			});
		});
		// 清空
		CHAIN_NAME_SET.clear();

		for (JSONObject flowJsonObject : flowJsonObjectList) {
			// 当存在<nodes>节点定义时，解析node节点
			if (flowJsonObject.getJSONObject(FLOW).containsKey(NODES)) {
				JSONArray nodeArrayList = flowJsonObject.getJSONObject(FLOW).getJSONObject(NODES).getJSONArray(NODE);
				String id, name, clazz, script, type, file;
				for (int i = 0; i < nodeArrayList.size(); i++) {
					JSONObject nodeObject = nodeArrayList.getJSONObject(i);
					id = nodeObject.getString(ID);
					name = nodeObject.getString(NAME);
					clazz = nodeObject.getString(_CLASS);
					type = nodeObject.getString(TYPE);
					script = nodeObject.getString(VALUE);
					file = nodeObject.getString(FILE);

					// 构建 node
					NodePropBean nodePropBean = new NodePropBean()
							.setId(id)
							.setName(name)
							.setClazz(clazz)
							.setScript(script)
							.setType(type)
							.setFile(file);

					buildNode(nodePropBean);
				}
			}

			//解析每一个chain
			JSONArray chainArray = flowJsonObject.getJSONObject(FLOW).getJSONArray(CHAIN);
			chainArray.forEach(o -> {
				JSONObject jsonObject = (JSONObject) o;
				parseOneChain(jsonObject);
			});
		}
	}


	/**
	 * 解析一个chain的过程
	 */
	private void parseOneChain(JSONObject chainObject) {
		String condValueStr;
		ConditionTypeEnum conditionType;
		String group;
		String errorResume;
		String any;
		String threadExecutorClass;

		//构建chainBuilder
		String chainName = chainObject.getString(NAME);
		LiteFlowChainBuilder chainBuilder = LiteFlowChainBuilder.createChain().setChainName(chainName);

		for (Object o : chainObject.getJSONArray(CONDITION)) {
			JSONObject condObject = (JSONObject) o;
			conditionType = ConditionTypeEnum.getEnumByCode(condObject.getString(TYPE));
			condValueStr = condObject.getString(VALUE);
			errorResume = condObject.getString(ERROR_RESUME);
			group = condObject.getString(GROUP);
			any = condObject.getString(ANY);
			threadExecutorClass = condObject.getString(THREAD_EXECUTOR_CLASS);

			ChainPropBean chainPropBean = new ChainPropBean()
					.setCondValueStr(condValueStr)
					.setGroup(group)
					.setErrorResume(errorResume)
					.setAny(any)
					.setThreadExecutorClass(threadExecutorClass)
					.setConditionType(conditionType);

			// 构建 chain
			buildChain(chainPropBean, chainBuilder);
		}
	}
}
