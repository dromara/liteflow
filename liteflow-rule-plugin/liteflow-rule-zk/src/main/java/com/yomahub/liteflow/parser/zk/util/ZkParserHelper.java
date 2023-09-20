package com.yomahub.liteflow.parser.zk.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.helper.NodeConvertHelper;
import com.yomahub.liteflow.parser.zk.exception.ZkException;
import com.yomahub.liteflow.parser.zk.vo.ZkParserVO;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ZkParserHelper {

	private static final Logger LOG = LoggerFactory.getLogger(ZkParserHelper.class);

	private final ZkParserVO zkParserVO;

	private final CuratorFramework client;

	private final String CHAIN_XML_PATTERN = "<chain name=\"{}\">{}</chain>";

	private final String NODE_XML_PATTERN = "<nodes>{}</nodes>";

	private final String NODE_ITEM_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\"><![CDATA[{}]]></node>";

	private final String NODE_ITEM_XML_WITH_LANGUAGE_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\" language=\"{}\"><![CDATA[{}]]></node>";

	private final String XML_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow>{}{}</flow>";

	public ZkParserHelper(ZkParserVO zkParserVO) {
		this.zkParserVO = zkParserVO;

		try {
			CuratorFramework client = CuratorFrameworkFactory.newClient(zkParserVO.getConnectStr(),
					new RetryNTimes(10, 5000));
			client.start();

			this.client = client;
		}
		catch (Exception e) {
			throw new ZkException(e.getMessage());
		}
	}

	public String getContent() {
		try {
			// 检查zk上有没有chainPath节点
			if (client.checkExists().forPath(zkParserVO.getChainPath()) == null) {
				throw new ZkException(StrUtil.format("zk node[{}] is not exist", zkParserVO.getChainPath()));
			}

			// 检查chainPath路径下有没有子节点
			List<String> chainNameList = client.getChildren().forPath(zkParserVO.getChainPath());
			if (CollectionUtil.isEmpty(chainNameList)) {
				throw new ZkException(StrUtil.format("There are no chains in path [{}]", zkParserVO.getChainPath()));
			}

			// 获取chainPath路径下的所有子节点内容List
			List<String> chainItemContentList = new ArrayList<>();
			for (String chainName : chainNameList) {
				String chainData = new String(
						client.getData().forPath(StrUtil.format("{}/{}", zkParserVO.getChainPath(), chainName)));
				chainItemContentList.add(StrUtil.format(CHAIN_XML_PATTERN, chainName, chainData));
			}
			// 合并成所有chain的xml内容
			String chainAllContent = CollUtil.join(chainItemContentList, StrUtil.EMPTY);

			// 检查是否有脚本内容，如果有，进行脚本内容的获取
			String scriptAllContent = StrUtil.EMPTY;
			if (hasScript()) {
				List<String> scriptNodeValueList = client.getChildren().forPath(zkParserVO.getScriptPath());

				List<String> scriptItemContentList = new ArrayList<>();
				for (String scriptNodeValue : scriptNodeValueList) {
					NodeConvertHelper.NodeSimpleVO nodeSimpleVO = NodeConvertHelper.convert(scriptNodeValue);
					if (Objects.isNull(nodeSimpleVO)) {
						throw new ZkException(StrUtil.format("The name of the zk node is invalid:{}", scriptNodeValue));
					}
					String scriptData = new String(client.getData()
						.forPath(StrUtil.format("{}/{}", zkParserVO.getScriptPath(), scriptNodeValue)));

					// 有语言类型
					if (StrUtil.isNotBlank(nodeSimpleVO.getLanguage())) {
						scriptItemContentList.add(StrUtil.format(NODE_ITEM_XML_WITH_LANGUAGE_PATTERN,
								nodeSimpleVO.getNodeId(), nodeSimpleVO.getName(), nodeSimpleVO.getType(),
								nodeSimpleVO.getLanguage(), scriptData));
					}
					// 没有语言类型
					else {
						scriptItemContentList.add(StrUtil.format(NODE_ITEM_XML_PATTERN, nodeSimpleVO.getNodeId(),
								nodeSimpleVO.getName(), nodeSimpleVO.getType(), scriptData));
					}
				}

				scriptAllContent = StrUtil.format(NODE_XML_PATTERN,
						CollUtil.join(scriptItemContentList, StrUtil.EMPTY));
			}

			return StrUtil.format(XML_PATTERN, scriptAllContent, chainAllContent);
		}
		catch (Exception e) {
			throw new ZkException(e.getMessage());
		}
	}

	public boolean hasScript() {
		// 没有配置scriptPath
		if (StrUtil.isBlank(zkParserVO.getScriptPath())) {
			return false;
		}

		try {
			// 配置了，但是不存在这个节点
			if (client.checkExists().forPath(zkParserVO.getScriptPath()) == null) {
				return false;
			}

			// 存在这个节点，但是子节点不存在
			List<String> chainNameList = client.getChildren().forPath(zkParserVO.getScriptPath());
			return !CollUtil.isEmpty(chainNameList);
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * 监听 zk 节点
	 */
	public void listenZkNode() {
		// 监听chain
		CuratorCache cache1 = CuratorCache.build(client, zkParserVO.getChainPath());
		cache1.start();
		cache1.listenable().addListener((type, oldData, data) -> {
			String path = data.getPath();
			String value = new String(data.getData());
			if (StrUtil.isBlank(value)) {
				return;
			}
			if (ListUtil.toList(CuratorCacheListener.Type.NODE_CREATED, CuratorCacheListener.Type.NODE_CHANGED)
				.contains(type)) {
				LOG.info("starting reload flow config... {} path={} value={},", type.name(), path, value);
				String chainName = FileNameUtil.getName(path);
				LiteFlowChainELBuilder.createChain().setChainId(chainName).setEL(value).build();
			}
			else if (CuratorCacheListener.Type.NODE_DELETED.equals(type)) {
				LOG.info("starting reload flow config... delete path={}", path);
				String chainName = FileNameUtil.getName(path);
				FlowBus.removeChain(chainName);
			}
		});

		if (StrUtil.isNotBlank(zkParserVO.getScriptPath())) {
			// 监听script
			CuratorCache cache2 = CuratorCache.build(client, zkParserVO.getScriptPath());
			cache2.start();
			cache2.listenable().addListener((type, oldData, data) -> {
				String path = data.getPath();
				String value = new String(data.getData());
				if (StrUtil.isBlank(value)) {
					return;
				}
				if (ListUtil.toList(CuratorCacheListener.Type.NODE_CREATED, CuratorCacheListener.Type.NODE_CHANGED)
					.contains(type)) {
					LOG.info("starting reload flow config... {} path={} value={},", type.name(), path, value);
					String scriptNodeValue = FileNameUtil.getName(path);
					NodeConvertHelper.NodeSimpleVO nodeSimpleVO = NodeConvertHelper.convert(scriptNodeValue);
					// 有语言类型
					if (StrUtil.isNotBlank(nodeSimpleVO.getLanguage())) {
						LiteFlowNodeBuilder.createScriptNode()
							.setId(nodeSimpleVO.getNodeId())
							.setType(NodeTypeEnum.getEnumByCode(nodeSimpleVO.getType()))
							.setName(nodeSimpleVO.getName())
							.setScript(value)
							.setLanguage(nodeSimpleVO.getLanguage())
							.build();
					}
					// 没有语言类型
					else {
						LiteFlowNodeBuilder.createScriptNode()
							.setId(nodeSimpleVO.getNodeId())
							.setType(NodeTypeEnum.getEnumByCode(nodeSimpleVO.getType()))
							.setName(nodeSimpleVO.getName())
							.setScript(value)
							.build();
					}
				}
				else if (CuratorCacheListener.Type.NODE_DELETED.equals(type)) {
					LOG.info("starting reload flow config... delete path={}", path);
					String scriptNodeValue = FileNameUtil.getName(path);
					NodeConvertHelper.NodeSimpleVO nodeSimpleVO = NodeConvertHelper.convert(scriptNodeValue);
					FlowBus.getNodeMap().remove(nodeSimpleVO.getNodeId());
				}
			});
		}
	}
}
