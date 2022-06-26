package com.yomahub.liteflow.parser.base;

import com.yomahub.liteflow.parser.ZookeeperXmlFlowParser;
import com.yomahub.liteflow.parser.helper.ZkParserHelper;
import org.apache.curator.framework.CuratorFramework;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * 基类，用于存放 ZookeeperXmlFlowELParser 通用方法
 *
 * @author tangkc
 */
public abstract class BaseZookeeperXmlFlowParser extends BaseXmlFlowParser {

	private static final Logger LOG = LoggerFactory.getLogger(ZookeeperXmlFlowParser.class);

	private final String nodePath;

	private final ZkParserHelper zkParserHelper;

	public BaseZookeeperXmlFlowParser(String node) {
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

		parse(content);

		zkParserHelper.listenZkNode(client);
	}

	/**
	 * 解析一个chain的过程
	 *
	 * @param chain 节点
	 */
	public abstract void parseOneChain(Element chain);
}
