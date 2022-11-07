package com.yomahub.liteflow.parser.etcd.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.etcd.EtcdClient;
import com.yomahub.liteflow.parser.etcd.exception.EtcdException;
import com.yomahub.liteflow.parser.etcd.vo.EtcdParserVO;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author zendwang
 * @since 2.9.0
 */
public class EtcdParserHelper {

	private static final Logger LOG = LoggerFactory.getLogger(EtcdParserHelper.class);

	private final String CHAIN_XML_PATTERN = "<chain name=\"{}\">{}</chain>";

	private final String NODE_XML_PATTERN = "<nodes>{}</nodes>";

	private final String NODE_ITEM_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\"><![CDATA[{}]]></node>";

	private final String XML_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow>{}{}</flow>";

	private static final String SEPARATOR = "/";

	private final EtcdParserVO etcdParserVO;

	private EtcdClient client;

	public EtcdParserHelper(EtcdParserVO etcdParserVO) {
		this.etcdParserVO = etcdParserVO;

		try{
			try{
				this.client  = ContextAwareHolder.loadContextAware().getBean(EtcdClient.class);
			}catch (Exception ignored){}
			if (this.client == null) {
				ClientBuilder clientBuilder = Client.builder()
						.endpoints(etcdParserVO.getEndpoints().split(","));
				if (StrUtil.isNotBlank(etcdParserVO.getNamespace())) {
					clientBuilder.namespace(ByteSequence.from(etcdParserVO.getNamespace(), CharsetUtil.CHARSET_UTF_8));
				}
				if (StrUtil.isAllNotBlank(etcdParserVO.getUser(), etcdParserVO.getPassword())) {
					clientBuilder.user(ByteSequence.from(etcdParserVO.getUser(), CharsetUtil.CHARSET_UTF_8));
					clientBuilder.password(ByteSequence.from(etcdParserVO.getPassword(), CharsetUtil.CHARSET_UTF_8));
				}
				this.client = new EtcdClient(clientBuilder.build());
			}
		}catch (Exception e){
			throw new EtcdException(e.getMessage());
		}
	}

	public String getContent(){
		try{
			//检查chainPath路径下有没有子节点
			List<String> chainNameList = client.getChildrenKeys(etcdParserVO.getChainPath(), SEPARATOR);
			if (CollectionUtil.isEmpty(chainNameList)){
				throw new EtcdException(StrUtil.format("There are no chains in path [{}]", etcdParserVO.getChainPath()));
			}

			//获取chainPath路径下的所有子节点内容List
			List<String> chainItemContentList = new ArrayList<>();
			for (String chainName : chainNameList){
				String chainData = client.get(StrUtil.format("{}/{}", etcdParserVO.getChainPath(), chainName));
				if (StrUtil.isNotBlank(chainData)) {
					chainItemContentList.add(StrUtil.format(CHAIN_XML_PATTERN, chainName, chainData));
				}
			}
			//合并成所有chain的xml内容
			String chainAllContent = CollUtil.join(chainItemContentList, StrUtil.EMPTY);

			//检查是否有脚本内容，如果有，进行脚本内容的获取
			String scriptAllContent = StrUtil.EMPTY;
			if (hasScript()){
				List<String> scriptNodeValueList = client.getChildrenKeys(etcdParserVO.getScriptPath(), SEPARATOR);

				List<String> scriptItemContentList = new ArrayList<>();
				for (String scriptNodeValue: scriptNodeValueList){
					NodeSimpleVO nodeSimpleVO = convert(scriptNodeValue);
					if (Objects.isNull(nodeSimpleVO)){
						throw new EtcdException(StrUtil.format("The name of the etcd node is invalid:{}", scriptNodeValue));
					}
					String scriptData = client.get(StrUtil.format("{}/{}", etcdParserVO.getScriptPath(), scriptNodeValue));

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
			throw new EtcdException(e.getMessage());
		}
	}

	public boolean hasScript(){
		//没有配置scriptPath
		if (StrUtil.isBlank(etcdParserVO.getScriptPath())){
			return false;
		}

		try{
			//存在这个节点，但是子节点不存在
			List<String> chainNameList = client.getChildrenKeys(etcdParserVO.getScriptPath(), SEPARATOR);
			if (CollUtil.isEmpty(chainNameList)){
				return false;
			}

			return true;
		}catch (Exception e){
			return false;
		}
	}


	/**
	 * 监听 etcd 节点
	 */
	public void listen(Consumer<String> parseConsumer) {
		this.client.watchChildChange(this.etcdParserVO.getChainPath(), (updatePath, updateValue) -> {
			LOG.info("update path={} value={},starting reload flow config...", updatePath, updateValue);
			parseConsumer.accept(getContent());
			}, (deletePath) -> {
			LOG.info("delete path={},starting reload flow config...", deletePath);
			parseConsumer.accept(getContent());
		});
		this.client.watchChildChange(this.etcdParserVO.getScriptPath(), (updatePath, updateValue) -> {
			LOG.info("update path={} value={},starting reload flow config...", updatePath, updateValue);
			parseConsumer.accept(getContent());
		}, (deletePath) -> {
			LOG.info("delete path={},starting reload flow config....", deletePath);
			parseConsumer.accept(getContent());
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
}
