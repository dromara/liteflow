package com.yomahub.liteflow.parser.base;

import com.alibaba.fastjson.JSONObject;
import com.yomahub.liteflow.parser.helper.ZkParserHelper;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * 基类，用于存放 ZookeeperYmlFlowELParser 通用方法
 *
 * @author tangkc
 */
public abstract class BaseZookeeperYmlFlowParser extends BaseYmlFlowParser{


	private static final Logger LOG = LoggerFactory.getLogger(BaseZookeeperYmlFlowParser.class);

	private final String nodePath;

	private final ZkParserHelper zkParserHelper;

	public BaseZookeeperYmlFlowParser(String node) {
		nodePath = node;
		Consumer<String> parseConsumer = t -> {
			try {
				parse(t);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
		zkParserHelper = new ZkParserHelper(nodePath, parseConsumer);
	}

	@Override
	public void parseMain(List<String> pathList) throws Exception {
		CuratorFramework client = zkParserHelper.getZkCuratorFramework(pathList);

		String content = new String(client.getData().forPath(nodePath));

		zkParserHelper.checkContent(content);

		JSONObject ruleObject = convertToJson(content);

		parse(ruleObject.toJSONString());

		zkParserHelper.listenZkNode(client);
	}

	/**
	 * 解析一个 chain 的过程
	 *
	 * @param chain chain
	 */
	public abstract void parseOneChain(JSONObject chain);
}
