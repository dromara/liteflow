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

	private final String XML_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow>{}{}</flow>";

	public ZkParserHelper(ZkParserVO zkParserVO) {
		this.zkParserVO = zkParserVO;

		try{
			CuratorFramework client = CuratorFrameworkFactory.newClient(
					zkParserVO.getConnectStr(),
					new RetryNTimes(10, 5000)
			);
			client.start();

			this.client = client;
		}catch (Exception e){
			throw new ZkException(e.getMessage());
		}
	}

	public String getContent(){
		try{
			//检查zk上有没有chainPath节点
			if (client.checkExists().forPath(zkParserVO.getChainPath()) == null) {
				throw new ZkException(StrUtil.format("zk node[{}] is not exist", zkParserVO.getChainPath()));
			}

			//检查chainPath路径下有没有子节点
			List<String> chainNameList = client.getChildren().forPath(zkParserVO.getChainPath());
			if (CollectionUtil.isEmpty(chainNameList)){
				throw new ZkException(StrUtil.format("There are no chains in path [{}]", zkParserVO.getChainPath()));
			}

			//获取chainPath路径下的所有子节点内容List
			List<String> chainItemContentList = new ArrayList<>();
			for (String chainName : chainNameList){
				String chainData = new String(client.getData().forPath(StrUtil.format("{}/{}", zkParserVO.getChainPath(), chainName)));
				chainItemContentList.add(StrUtil.format(CHAIN_XML_PATTERN, chainName, chainData));
			}
			//合并成所有chain的xml内容
			String chainAllContent = CollUtil.join(chainItemContentList, StrUtil.EMPTY);

			//检查是否有脚本内容，如果有，进行脚本内容的获取
			String scriptAllContent = StrUtil.EMPTY;
			if (hasScript()){
				List<String> scriptNodeValueList = client.getChildren().forPath(zkParserVO.getScriptPath());

				List<String> scriptItemContentList = new ArrayList<>();
				for (String scriptNodeValue: scriptNodeValueList){
					NodeSimpleVO nodeSimpleVO = convert(scriptNodeValue);
					if (Objects.isNull(nodeSimpleVO)){
						throw new ZkException(StrUtil.format("The name of the zk node is invalid:{}", scriptNodeValue));
					}
					String scriptData = new String(
							client.getData().forPath(StrUtil.format("{}/{}", zkParserVO.getScriptPath(), scriptNodeValue))
					);

					scriptItemContentList.add(
							StrUtil.format(NODE_ITEM_XML_PATTERN,
									nodeSimpleVO.getNodeId(),
									nodeSimpleVO.getName(),
									nodeSimpleVO.getType(),
									scriptData)
					);
				}

				scriptAllContent = StrUtil.format(NODE_XML_PATTERN, CollUtil.join(scriptItemContentList, StrUtil.EMPTY));
			}

			return StrUtil.format(XML_PATTERN, scriptAllContent, chainAllContent);
		}catch (Exception e){
			throw new ZkException(e.getMessage());
		}
	}

	public boolean hasScript(){
		//没有配置scriptPath
		if (StrUtil.isBlank(zkParserVO.getScriptPath())){
			return false;
		}

		try{
			//配置了，但是不存在这个节点
			if (client.checkExists().forPath(zkParserVO.getScriptPath()) == null){
				return false;
			}

			//存在这个节点，但是子节点不存在
			List<String> chainNameList = client.getChildren().forPath(zkParserVO.getScriptPath());
			return !CollUtil.isEmpty(chainNameList);
		}catch (Exception e){
			return false;
		}
	}

	/**
	 * 监听 zk 节点
	 */
	public void listenZkNode(){
		//监听chain
		CuratorCache cache1 = CuratorCache.build(client, zkParserVO.getChainPath());
		cache1.start();
		cache1.listenable().addListener((type, oldData, data) -> {
			String path = data.getPath();
			String value = new String(data.getData());
			if (StrUtil.isBlank(value)){
				return;
			}
			if (ListUtil.toList(CuratorCacheListener.Type.NODE_CREATED, CuratorCacheListener.Type.NODE_CHANGED).contains(type)){
				LOG.info("starting reload flow config... {} path={} value={},", type.name(), path, value);
				String chainName = FileNameUtil.getName(path);
				LiteFlowChainELBuilder.createChain().setChainId(chainName).setEL(value).build();
			}else if(CuratorCacheListener.Type.NODE_DELETED.equals(type)){
				LOG.info("starting reload flow config... delete path={}", path);
				String chainName = FileNameUtil.getName(path);
				FlowBus.removeChain(chainName);
			}
		});

		//监听script
		CuratorCache cache2 = CuratorCache.build(client, zkParserVO.getScriptPath());
		cache2.start();
		cache2.listenable().addListener((type, oldData, data) -> {
			String path = data.getPath();
			String value = new String(data.getData());
			if (StrUtil.isBlank(value)){
				return;
			}
			if (ListUtil.toList(CuratorCacheListener.Type.NODE_CREATED, CuratorCacheListener.Type.NODE_CHANGED).contains(type)){
				LOG.info("starting reload flow config... {} path={} value={},", type.name(), path, value);
				String scriptNodeValue = FileNameUtil.getName(path);
				NodeSimpleVO nodeSimpleVO = convert(scriptNodeValue);
				LiteFlowNodeBuilder.createScriptNode().setId(nodeSimpleVO.getNodeId())
						.setType(NodeTypeEnum.getEnumByCode(nodeSimpleVO.type))
						.setName(nodeSimpleVO.getName())
						.setScript(value).build();
			} else if (CuratorCacheListener.Type.NODE_DELETED.equals(type)) {
				LOG.info("starting reload flow config... delete path={}", path);
				String scriptNodeValue = FileNameUtil.getName(path);
				NodeSimpleVO nodeSimpleVO = convert(scriptNodeValue);
				FlowBus.getNodeMap().remove(nodeSimpleVO.getNodeId());
			}
		});
	}

	public NodeSimpleVO convert(String str){
		//不需要去理解这串正则，就是一个匹配冒号的
		//一定得是a:b，或是a:b:c...这种完整类型的字符串的
		List<String> matchItemList = ReUtil.findAllGroup0("(?<=[^:]:)[^:]+|[^:]+(?=:[^:])", str);
		if (CollUtil.isEmpty(matchItemList)){
			return null;
		}

		NodeSimpleVO nodeSimpleVO = new NodeSimpleVO();
		if (matchItemList.size() > 1){
			nodeSimpleVO.setNodeId(matchItemList.get(0));
			nodeSimpleVO.setType(matchItemList.get(1));
		}

		if (matchItemList.size() > 2){
			nodeSimpleVO.setName(matchItemList.get(2));
		}

		return nodeSimpleVO;
	}

	private static class NodeSimpleVO{

		private String nodeId;

		private String type;

		private String name="";

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
	}

	public static void main(String[] args) {
		System.out.println(FileNameUtil.getName("/chain/dadfa/c1"));
	}
}
