package com.yomahub.liteflow.parser.helper;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.builder.prop.NodePropBean;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.*;
import com.yomahub.liteflow.flow.FlowBus;
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
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
			});
		});
		// 清空
		chainNameSet.clear();

		// 解析每一个chain
		for (Document document : documentList) {
			Element rootElement = document.getRootElement();
			List<Element> chainList = rootElement.elements(CHAIN);
			chainList.forEach(parseOneChainConsumer);
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
			}
		});
		// 清空
		chainNameSet.clear();

		for (JsonNode flowJsonNode : flowJsonObjectList) {
			// 解析每一个chain
			Iterator<JsonNode> chainIterator = flowJsonNode.get(FLOW).get(CHAIN).elements();
			while (chainIterator.hasNext()) {
				JsonNode jsonNode = chainIterator.next();
				parseOneChainConsumer.accept(jsonNode);
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
		LiteFlowChainELBuilder.createChain()
				.setChainId(chainId)
				.setEL(el)
				.build();
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
		LiteFlowChainELBuilder.createChain()
				.setChainId(chainId)
				.setEL(el)
				.build();
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

	private static class RegexUtil {

		// java 注释的正则表达式
		private static final String REGEX_COMMENT = "(?<!(:|@))\\/\\/.*|\\/\\*(\\s|.)*?\\*\\/";

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

	}

}
