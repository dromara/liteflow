package com.yomahub.liteflow.parser.helper;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.exception.ParseException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;
import java.util.function.Consumer;

public class ZkParserHelper {

	private static final Logger LOG = LoggerFactory.getLogger(ZkParserHelper.class);

	private final String nodePath;
	private final Consumer<String> parseConsumer;

	public ZkParserHelper(String node, Consumer<String> parseConsumer) {
		this.nodePath = node;
		this.parseConsumer = parseConsumer;
	}

	/**
	 * 获取zk客户端
	 *
	 * @param pathList zk路径
	 * @return
	 * @throws Exception
	 */
	public CuratorFramework getZkCuratorFramework(List<String> pathList) throws Exception {
		//zk不允许有多个path
		String path = pathList.get(0);
		CuratorFramework client = CuratorFrameworkFactory.newClient(
				path,
				new RetryNTimes(10, 5000)
		);
		client.start();

		if (client.checkExists().forPath(nodePath) == null) {
			client.create().creatingParentsIfNeeded().forPath(nodePath, "".getBytes());
		}
		return client;
	}

	/**
	 * 检查 content 是否合法
	 *
	 * @param content 内容
	 */
	public void checkContent(String content) {
		if (StrUtil.isBlank(content)) {
			String error = MessageFormat.format("the node[{0}] value is empty", nodePath);
			throw new ParseException(error);
		}
	}

	/**
	 * 监听 zk 节点
	 *
	 * @param client zk 客户端
	 * @throws Exception
	 */
	public void listenZkNode(CuratorFramework client) throws Exception {
		final NodeCache cache = new NodeCache(client, nodePath);
		cache.start();

		cache.getListenable().addListener(() -> {
			String content1 = new String(cache.getCurrentData().getData());
			LOG.info("stating load flow config....");
			parseConsumer.accept(content1);
		});
	}

}
