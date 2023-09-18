package com.yomahub.liteflow.parser.helper;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.builder.prop.NodePropBean;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.*;
import com.yomahub.liteflow.flow.FlowBus;
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.yomahub.liteflow.common.ChainConstant.*;

/**
 * Parser 通用 Helper
 *
 * @author tangkc
 */
public class ParserHelper {

	/**
	 * 私有化构造器
	 */
	private ParserHelper() {
	}

	/**
	 * 构建 node
	 * @param nodePropBean 构建 node 的中间属性
	 */
	public static void buildNode(NodePropBean nodePropBean) {
		String id = nodePropBean.getId();
		String name = nodePropBean.getName();
		String clazz = nodePropBean.getClazz();
		String script = nodePropBean.getScript();
		String type = nodePropBean.getType();
		String file = nodePropBean.getFile();
		String language = nodePropBean.getLanguage();

		// clazz有值的，基本都不是脚本节点
		// 脚本节点，都必须配置type
		// 非脚本节点的先尝试自动推断类型
		if (StrUtil.isNotBlank(clazz)) {
			try {
				// 先尝试从继承的类型中推断
				Class<?> c = Class.forName(clazz);
				NodeTypeEnum nodeType = NodeTypeEnum.guessType(c);
				if (nodeType != null) {
					type = nodeType.getCode();
				}
			}
			catch (Exception e) {
				throw new NodeClassNotFoundException(StrUtil.format("cannot find the node[{}]", clazz));
			}
		}

		// 因为脚本节点是必须设置type的，所以到这里type就全都有了，所以进行二次检查
		if (StrUtil.isBlank(type)) {
			throw new NodeTypeCanNotGuessException(StrUtil.format("cannot guess the type of node[{}]", clazz));
		}

		// 检查nodeType是不是规定的类型
		NodeTypeEnum nodeTypeEnum = NodeTypeEnum.getEnumByCode(type);
		if (ObjectUtil.isNull(nodeTypeEnum)) {
			throw new NodeTypeNotSupportException(StrUtil.format("type [{}] is not support", type));
		}

		// 进行node的build过程
		LiteFlowNodeBuilder.createNode()
			.setId(id)
			.setName(name)
			.setClazz(clazz)
			.setType(nodeTypeEnum)
			.setScript(script)
			.setFile(file)
			.setLanguage(language)
			.build();
	}

	/**
	 * xml 形式的主要解析过程
	 * @param documentList documentList
	 */
	/**
	 * xml 形式的主要解析过程
	 * @param documentList documentList
	 */
	public static void parseNodeDocument(List<Document> documentList) {
		for (Document document : documentList) {
			Element rootElement = document.getRootElement();
			Element nodesElement = rootElement.element(NODES);
			// 当存在<nodes>节点定义时，解析node节点
			if (ObjectUtil.isNotNull(nodesElement)) {
				List<Element> nodeList = nodesElement.elements(NODE);
				String id, name, clazz, type, script, file, language;
				for (Element e : nodeList) {
					id = e.attributeValue(ID);
					name = e.attributeValue(NAME);
					clazz = e.attributeValue(_CLASS);
					type = e.attributeValue(TYPE);
					script = e.getText();
					file = e.attributeValue(FILE);
					language = e.attributeValue(LANGUAGE);

					// 构建 node
					NodePropBean nodePropBean = new NodePropBean().setId(id)
						.setName(name)
						.setClazz(clazz)
						.setScript(script)
						.setType(type)
						.setFile(file)
						.setLanguage(language);

					ParserHelper.buildNode(nodePropBean);
				}
			}
		}
	}

	public static void parseChainDocument(List<Document> documentList, Set<String> chainNameSet,
			Consumer<Element> parseOneChainConsumer) {
		//用于存放抽象chain的map
		Map<String,Element> abstratChainMap = new HashMap<>();
		//用于存放已经解析过的实现chain
		Set<Element> implChainSet = new HashSet<>();
		// 先在元数据里放上chain
		// 先放有一个好处，可以在parse的时候先映射到FlowBus的chainMap，然后再去解析
		// 这样就不用去像之前的版本那样回归调用
		// 同时也解决了不能循环依赖的问题
		documentList.forEach(document -> {
			// 解析chain节点
			List<Element> chainList = document.getRootElement().elements(CHAIN);

			// 先在元数据里放上chain
			chainList.forEach(e -> {
				// 校验加载的 chainName 是否有重复的
				// TODO 这里是否有个问题，当混合格式加载的时候，2个同名的Chain在不同的文件里，就不行了
				String chainName = Optional.ofNullable(e.attributeValue(ID)).orElse(e.attributeValue(NAME));
				// 检查 chainName
				checkChainId(chainName, e.getText());
				if (!chainNameSet.add(chainName)) {
					throw new ChainDuplicateException(String.format("[chain name duplicate] chainName=%s", chainName));
				}

				FlowBus.addChain(chainName);
				if(e.attributeValue(ABSTRACT) != null && e.attributeValue(ABSTRACT).equals("true")){
					abstratChainMap.put(chainName,e);
				}
			});
		});
		// 清空
		chainNameSet.clear();

		// 解析每一个chain
		for (Document document : documentList) {
			Element rootElement = document.getRootElement();
			List<Element> chainList = rootElement.elements(CHAIN);
			//先对继承自抽象Chain的chain进行字符串替换
			chainList.stream()
					.filter(e -> e.attributeValue(EXTENDS)!=null)
					.forEach(e->{
						String baseChainId = e.attributeValue(EXTENDS);
						if(abstratChainMap.containsKey(baseChainId)) {
							Element baseChain = abstratChainMap.get(baseChainId);
							parseImplChain(baseChain,e,abstratChainMap,implChainSet);
						}else{
							throw new ChainNotFoundException(String.format("[abstract chain not found] chainName=%s", baseChainId));
						}
					});
			//对所有非抽象chain进行解析
			chainList.stream()
					.filter(e -> e.attributeValue(ABSTRACT)==null || e.attributeValue(ABSTRACT).equals("false"))
					.forEach(parseOneChainConsumer);
		}
	}

	public static void parseNodeJson(List<JsonNode> flowJsonObjectList) {
		for (JsonNode flowJsonNode : flowJsonObjectList) {
			// 当存在<nodes>节点定义时，解析node节点
			if (flowJsonNode.get(FLOW).has(NODES)) {
				Iterator<JsonNode> nodeIterator = flowJsonNode.get(FLOW).get(NODES).get(NODE).elements();
				String id, name, clazz, script, type, file;
				while ((nodeIterator.hasNext())) {
					JsonNode nodeObject = nodeIterator.next();
					id = nodeObject.get(ID).textValue();
					name = nodeObject.hasNonNull(NAME) ? nodeObject.get(NAME).textValue() : "";
					clazz = nodeObject.hasNonNull(_CLASS) ? nodeObject.get(_CLASS).textValue() : "";
					;
					type = nodeObject.hasNonNull(TYPE) ? nodeObject.get(TYPE).textValue() : null;
					script = nodeObject.hasNonNull(VALUE) ? nodeObject.get(VALUE).textValue() : "";
					file = nodeObject.hasNonNull(FILE) ? nodeObject.get(FILE).textValue() : "";

					// 构建 node
					NodePropBean nodePropBean = new NodePropBean().setId(id)
						.setName(name)
						.setClazz(clazz)
						.setScript(script)
						.setType(type)
						.setFile(file);

					ParserHelper.buildNode(nodePropBean);
				}
			}
		}
	}

	public static void parseChainJson(List<JsonNode> flowJsonObjectList, Set<String> chainNameSet,
			Consumer<JsonNode> parseOneChainConsumer) {
		//用于存放抽象chain的map
		Map<String,JsonNode> abstratChainMap = new HashMap<>();
		//用于存放已经解析过的实现chain
		Set<JsonNode> implChainSet = new HashSet<>();
		// 先在元数据里放上chain
		// 先放有一个好处，可以在parse的时候先映射到FlowBus的chainMap，然后再去解析
		// 这样就不用去像之前的版本那样回归调用
		// 同时也解决了不能循环依赖的问题
		flowJsonObjectList.forEach(jsonObject -> {
			// 解析chain节点
			Iterator<JsonNode> iterator = jsonObject.get(FLOW).get(CHAIN).elements();
			// 先在元数据里放上chain
			while (iterator.hasNext()) {
				JsonNode innerJsonObject = iterator.next();
				// 校验加载的 chainName 是否有重复的
				// TODO 这里是否有个问题，当混合格式加载的时候，2个同名的Chain在不同的文件里，就不行了
				JsonNode chainNameJsonNode = Optional.ofNullable(innerJsonObject.get(ID))
					.orElse(innerJsonObject.get(NAME));
				String chainName = Optional.ofNullable(chainNameJsonNode).map(JsonNode::textValue).orElse(null);
				// 检查 chainName
				checkChainId(chainName, innerJsonObject.toPrettyString());
				if (!chainNameSet.add(chainName)) {
					throw new ChainDuplicateException(String.format("[chain name duplicate] chainName=%s", chainName));
				}

				FlowBus.addChain(chainName);

				if(innerJsonObject.hasNonNull(ABSTRACT) && innerJsonObject.get(ABSTRACT).asBoolean()) {
					abstratChainMap.put(chainName,innerJsonObject);
				}
			}
		});
		// 清空
		chainNameSet.clear();

		for (JsonNode flowJsonNode : flowJsonObjectList) {
			// 解析每一个chain
			Iterator<JsonNode> chainIterator = flowJsonNode.get(FLOW).get(CHAIN).elements();
			while (chainIterator.hasNext()) {
				JsonNode chainNode = chainIterator.next();
				//首先需要对继承自抽象Chain的chain进行字符串替换
				if(chainNode.hasNonNull(EXTENDS)){
					String baseChainId = chainNode.get(EXTENDS).textValue();
					if(abstratChainMap.containsKey(baseChainId)) {
						JsonNode baseChain = abstratChainMap.get(baseChainId);
						parseImplChain(baseChain,chainNode,abstratChainMap,implChainSet);
					}else{
						throw new ChainNotFoundException(String.format("[abstract chain not found] chainName=%s", baseChainId));
					}
				}
				//如果一个chain不为抽象chain，则进行解析
				if(chainNode.get(ABSTRACT) == null || !chainNode.get(ABSTRACT).asBoolean()){
					parseOneChainConsumer.accept(chainNode);
				}
			}
		}
	}

	/**
	 * 解析一个chain的过程
	 * @param chainNode chain 节点
	 */
	public static void parseOneChainEl(JsonNode chainNode) {
		// 构建chainBuilder
		String chainId = Optional.ofNullable(chainNode.get(ID)).orElse(chainNode.get(NAME)).textValue();
		String el = chainNode.get(VALUE).textValue();
		LiteFlowChainELBuilder chainELBuilder = LiteFlowChainELBuilder.createChain().setChainId(chainId);
		chainELBuilder.setEL(el).build();
	}

	/**
	 * 解析一个chain的过程
	 * @param e chain 节点
	 */
	public static void parseOneChainEl(Element e) {
		// 构建chainBuilder
		String chainId = Optional.ofNullable(e.attributeValue(ID)).orElse(e.attributeValue(NAME));
		String text = e.getText();
		String el = RegexUtil.removeComments(text);
		LiteFlowChainELBuilder chainELBuilder = LiteFlowChainELBuilder.createChain().setChainId(chainId);
		chainELBuilder.setEL(el).build();
	}

	/**
	 * 检查 chainId
	 * @param chainId chainId
	 * @param elData elData
	 */
	private static void checkChainId(String chainId, String elData) {
		if (StrUtil.isBlank(chainId)) {
			throw new ParseException("missing chain id in expression \r\n" + elData);
		}
	}

	/**
	 * 解析一个继承自baseChain的implChain,xml格式
	 * @param baseChain 父Chain
	 * @param implChain 实现Chain
	 * @param abstractChainMap 所有的抽象Chain
	 * @param implChainSet 已经解析过的实现Chain
	 */
	private static void parseImplChain(JsonNode baseChain,JsonNode implChain,Map<String,JsonNode> abstractChainMap,Set<JsonNode> implChainSet) {
		//如果已经解析过了，就不再解析
		if(implChainSet.contains(implChain)) return;
		//如果baseChainId也是继承自其他的chain，需要递归解析
		if(baseChain.get(EXTENDS)!=null){
			String pBaseChainId = baseChain.get(EXTENDS).textValue();
			if(abstractChainMap.containsKey(pBaseChainId)) {
				JsonNode pBaseChain = abstractChainMap.get(pBaseChainId);
				parseImplChain(pBaseChain, baseChain, abstractChainMap, implChainSet);
			}else{
				throw new ChainNotFoundException(String.format("[abstract chain not found] chainName=%s", pBaseChainId));
			}
		}
		//否则根据baseChainId解析implChainId
		String implChainEl = implChain.get(VALUE).textValue();
		String baseChainEl = baseChain.get(VALUE).textValue();
		//替换baseChainId中的implChainId
		// 使用正则表达式匹配占位符并替换
		String parsedEl = RegexUtil.replaceAbstractChain(baseChainEl,implChainEl);
		ObjectNode objectNode = (ObjectNode) implChain;
		objectNode.put(VALUE,parsedEl);
		implChainSet.add(implChain);
	}

	/**
	 * 解析一个继承自baseChain的implChain,json格式
	 * @param baseChain 父Chain
	 * @param implChain 实现Chain
	 * @param abstractChainMap 所有的抽象Chain
	 * @param implChainSet 已经解析过的实现Chain
	 */
	private static void parseImplChain(Element baseChain,Element implChain,Map<String,Element> abstractChainMap,Set<Element> implChainSet) {
		//如果已经解析过了，就不再解析
		if(implChainSet.contains(implChain)) return;
		//如果baseChainId也是继承自其他的chain，需要递归解析
		if(baseChain.attributeValue(EXTENDS)!=null){
			String pBaseChainId = baseChain.attributeValue(EXTENDS);
			if(abstractChainMap.containsKey(pBaseChainId)) {
				Element pBaseChain = abstractChainMap.get(pBaseChainId);
				parseImplChain(pBaseChain, baseChain, abstractChainMap, implChainSet);
			}else{
				throw new ChainNotFoundException(String.format("[abstract chain not found] chainName=%s", pBaseChainId));
			}
		}
		//否则根据baseChainId解析implChainId
		String implChainEl = implChain.getText();
		String baseChainEl = baseChain.getText();
		//替换baseChainId中的implChainId
		// 使用正则表达式匹配占位符并替换
		String parsedEl = RegexUtil.replaceAbstractChain(baseChainEl,implChainEl);
		implChain.setText(parsedEl);
		implChainSet.add(implChain);
	}

	private static class RegexUtil {

		// java 注释的正则表达式
		private static final String REGEX_COMMENT = "(?<!(:|@))\\/\\/.*|\\/\\*(\\s|.)*?\\*\\/";

		// abstractChain 占位符正则表达式
		private static final String REGEX_ABSTRACT_HOLDER = "\\{(\\d+)\\}";


		/**
		 * 移除 el 表达式中的注释，支持 java 的注释，包括单行注释、多行注释， 会压缩字符串，移除空格和换行符
		 * @param elStr el 表达式
		 * @return 移除注释后的 el 表达式
		 */
		private static String removeComments(String elStr) {
			if (StrUtil.isBlank(elStr)) {
				return elStr;
			}

			return Pattern.compile(REGEX_COMMENT)
				.matcher(elStr)
				// 移除注释
				.replaceAll(CharSequenceUtil.EMPTY);
		}

		/**
		 * 根据抽象EL和实现EL，替换抽象EL中的占位符
		 * @param abstractChain 抽象EL
		 * @param implChain 抽象EL对应的一个实现
		 * @return 替换后的EL
		 */
		private static String replaceAbstractChain(String abstractChain,String implChain){
			//匹配抽象chain的占位符
			Pattern placeHolder = Pattern.compile(REGEX_ABSTRACT_HOLDER);
			Matcher placeHolderMatcher = placeHolder.matcher(abstractChain);
			while(placeHolderMatcher.find()){
				//到implChain中找到对应的占位符实现
				int index = Integer.parseInt(placeHolderMatcher.group(1));
				Pattern placeHolderImpl = Pattern.compile("\\{" + index + "\\}=(.*?)(;|$)");
				Matcher implMatcher = placeHolderImpl.matcher(implChain);
				if (implMatcher.find()) {
					String replacement = implMatcher.group(1).trim();
					abstractChain = abstractChain.replace("{" + index + "}", replacement);
				}else{
					throw new ParseException("missing abstract chain in expression \r\n" + abstractChain);
				}
			}
			return abstractChain;
		}
	}

}
