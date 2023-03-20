package com.yomahub.liteflow.parser.apollo.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.apollo.exception.ApolloException;
import com.yomahub.liteflow.parser.apollo.vo.ApolloParserConfigVO;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zhanghua
 * @since 2.9.5
 */
public class ApolloParseHelper {

	private static final Logger LOG = LoggerFactory.getLogger(ApolloParseHelper.class);

	private final String CHAIN_XML_PATTERN = "<chain name=\"{}\">{}</chain>";

	private final String NODE_XML_PATTERN = "<nodes>{}</nodes>";

	private final String NODE_ITEM_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\"><![CDATA[{}]]></node>";

	private final String XML_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow>{}{}</flow>";

	private final ApolloParserConfigVO apolloParserConfigVO;

	private Config chainConfig;

	private Config scriptConfig;

	public ApolloParseHelper(ApolloParserConfigVO apolloParserConfigVO) {
		this.apolloParserConfigVO = apolloParserConfigVO;

		try {
			try {
				// 这里本身对于程序运行来说没有什么意义，拿到的永远是null
				// 其实config对象也没有注入到spring容器中
				// 这里这样写的目的是为了单测中的mockito，当有@MockBean的时候，这里就能拿到了
				this.chainConfig = ContextAwareHolder.loadContextAware().getBean("chainConfig");
				this.scriptConfig = ContextAwareHolder.loadContextAware().getBean("scriptConfig");
			}
			catch (Exception ignored) {
			}

			if (ObjectUtil.isNull(chainConfig)) {
				chainConfig = ConfigService.getConfig(apolloParserConfigVO.getChainNamespace());
				String scriptNamespace;
				// scriptConfig is optional
				if (StrUtil.isNotBlank(scriptNamespace = apolloParserConfigVO.getScriptNamespace())) {
					scriptConfig = ConfigService.getConfig(scriptNamespace);
				}
			}
		}
		catch (Exception e) {
			throw new ApolloException(e.getMessage());
		}
	}

	public String getContent() {

		try {
			// 1. handle chain
			Set<String> propertyNames = chainConfig.getPropertyNames();
			if (CollectionUtil.isEmpty(propertyNames)) {
				throw new ApolloException(StrUtil.format("There are no chains in namespace : {}",
						apolloParserConfigVO.getChainNamespace()));
			}
			List<String> chainItemContentList = propertyNames.stream()
				.map(item -> StrUtil.format(CHAIN_XML_PATTERN, item, chainConfig.getProperty(item, StrUtil.EMPTY)))
				.collect(Collectors.toList());
			// merge all chain content
			String chainAllContent = CollUtil.join(chainItemContentList, StrUtil.EMPTY);

			// 2. handle script if needed
			String scriptAllContent = StrUtil.EMPTY;
			Set<String> scriptNamespaces;
			if (Objects.nonNull(scriptConfig)
					&& CollectionUtil.isNotEmpty(scriptNamespaces = scriptConfig.getPropertyNames())) {

				List<String> scriptItemContentList = scriptNamespaces.stream()
					.map(item -> convert(item, scriptConfig.getProperty(item, StrUtil.EMPTY)))
					.filter(Objects::nonNull)
					.map(item -> StrUtil.format(NODE_ITEM_XML_PATTERN, item.getNodeId(), item.getName(), item.getType(),
							item.getScript()))
					.collect(Collectors.toList());

				scriptAllContent = StrUtil.format(NODE_XML_PATTERN,
						CollUtil.join(scriptItemContentList, StrUtil.EMPTY));
			}

			return StrUtil.format(XML_PATTERN, scriptAllContent, chainAllContent);
		}
		catch (Exception e) {
			throw new ApolloException(e.getMessage());
		}
	}

	/**
	 * listen apollo config change
	 */
	public void listenApollo() {

		// chain
		chainConfig.addChangeListener(changeEvent -> changeEvent.changedKeys().forEach(changeKey -> {
			ConfigChange configChange = changeEvent.getChange(changeKey);
			String newValue = configChange.getNewValue();
			PropertyChangeType changeType = configChange.getChangeType();
			switch (changeType) {
				case ADDED:
				case MODIFIED:
					LOG.info("starting reload flow config... {} key={} value={},", changeType.name(), changeKey,
							newValue);
					LiteFlowChainELBuilder.createChain().setChainId(changeKey).setEL(newValue).build();
					break;
				case DELETED:
					LOG.info("starting reload flow config... delete key={}", changeKey);
					FlowBus.removeChain(changeKey);

			}
		}));

		if (StrUtil.isNotBlank(apolloParserConfigVO.getScriptNamespace())) {
			scriptConfig.addChangeListener(changeEvent -> changeEvent.changedKeys().forEach(changeKey -> {
				ConfigChange configChange = changeEvent.getChange(changeKey);
				String newValue = configChange.getNewValue();

				PropertyChangeType changeType = configChange.getChangeType();

				NodeSimpleVO nodeSimpleVO;
				switch (changeType) {
					case ADDED:
					case MODIFIED:
						LOG.info("starting reload flow config... {} key={} value={},", changeType.name(), changeKey,
								newValue);
						nodeSimpleVO = convert(changeKey, newValue);
						LiteFlowNodeBuilder.createScriptNode()
							.setId(nodeSimpleVO.getNodeId())
							.setType(NodeTypeEnum.getEnumByCode(nodeSimpleVO.getType()))
							.setName(nodeSimpleVO.getName())
							.setScript(nodeSimpleVO.getScript())
							.build();
						break;
					case DELETED:
						LOG.info("starting reload flow config... delete key={}", changeKey);
						nodeSimpleVO = convert(changeKey, null);
						FlowBus.getNodeMap().remove(nodeSimpleVO.getNodeId());
				}
			}));
		}
	}

	private NodeSimpleVO convert(String key, String value) {
		// 不需要去理解这串正则，就是一个匹配冒号的
		// 一定得是a:b，或是a:b:c...这种完整类型的字符串的
		List<String> matchItemList = ReUtil.findAllGroup0("(?<=[^:]:)[^:]+|[^:]+(?=:[^:])", key);
		if (CollUtil.isEmpty(matchItemList)) {
			return null;
		}

		NodeSimpleVO nodeSimpleVO = new NodeSimpleVO();
		if (matchItemList.size() > 1) {
			nodeSimpleVO.setNodeId(matchItemList.get(0));
			nodeSimpleVO.setType(matchItemList.get(1));
		}

		if (matchItemList.size() > 2) {
			nodeSimpleVO.setName(matchItemList.get(2));
		}

		// set script
		nodeSimpleVO.setScript(value);

		return nodeSimpleVO;
	}

	private static class NodeSimpleVO {

		private String nodeId;

		private String type;

		private String name = StrUtil.EMPTY;

		private String script;

		public String getNodeId() {
			return nodeId;
		}

		public void setNodeId(String nodeId) {
			this.nodeId = nodeId;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getScript() {
			return script;
		}

		public void setScript(String script) {
			this.script = script;
		}

	}

}
