package com.yomahub.liteflow.parser.zk.util;

import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.exception.ParseException;
import com.yomahub.liteflow.parser.zk.exception.ZkException;
import com.yomahub.liteflow.parser.zk.vo.ZkParserVO;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.function.Consumer;

public class ZkParserHelper {

	private static final Logger LOG = LoggerFactory.getLogger(ZkParserHelper.class);

	private final ZkParserVO zkParserVO;
	private final Consumer<String> parseConsumer;

	private final CuratorFramework client;

	public ZkParserHelper(ZkParserVO zkParserVO, Consumer<String> parseConsumer) {
		this.zkParserVO = zkParserVO;
		this.parseConsumer = parseConsumer;

		try{
			CuratorFramework client = CuratorFrameworkFactory.newClient(
					zkParserVO.getConnectStr(),
					new RetryNTimes(10, 5000)
			);
			client.start();

			if (client.checkExists().forPath(zkParserVO.getNodePath()) == null) {
				client.create().creatingParentsIfNeeded().forPath(zkParserVO.getNodePath(), "".getBytes());
			}
			this.client = client;
		}catch (Exception e){
			throw new ZkException(e.getMessage());
		}

	}

	public String getContent(){
		try{
			return new String(client.getData().forPath(zkParserVO.getNodePath()));
		}catch (Exception e){
			throw new ZkException(e.getMessage());
		}
	}

	/**
	 * 检查 content 是否合法
	 */
	public void checkContent(String content) {
		if (StrUtil.isBlank(content)) {
			String error = MessageFormat.format("the node[{0}] value is empty", zkParserVO.getNodePath());
			throw new ParseException(error);
		}
	}

	/**
	 * 监听 zk 节点
	 */
	public void listenZkNode() throws Exception {
		final NodeCache cache = new NodeCache(client, zkParserVO.getNodePath());
		cache.start();

		cache.getListenable().addListener(() -> {
			String content1 = new String(cache.getCurrentData().getData());
			LOG.info("stating load flow config....");
			parseConsumer.accept(content1);
		});
	}

}
