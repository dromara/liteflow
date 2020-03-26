package com.yomahub.liteflow.parser;

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yomahub.liteflow.exception.ParseException;

public class ZookeeperXmlFlowParser extends XmlFlowParser{

	private static final Logger LOG = LoggerFactory.getLogger(ZookeeperXmlFlowParser.class);

	private String nodePath = "/lite-flow/flow";

	public ZookeeperXmlFlowParser() {

	}

	public ZookeeperXmlFlowParser(String node) {
		nodePath = node;
	}

	@Override
	public void parseMain(String path) throws Exception {
		CuratorFramework client = CuratorFrameworkFactory.newClient(
				path,
                new RetryNTimes(10, 5000)
        );
        client.start();

        if (client.checkExists().forPath(nodePath) == null) {
        	client.create().creatingParentsIfNeeded().forPath(nodePath, "".getBytes());
        }

        String content = new String(client.getData().forPath(nodePath));


        if(StringUtils.isBlank(content)) {
        	String error = MessageFormat.format("the node[{0}] value is empty", nodePath);
        	throw new ParseException(error);
        }
        parse(content);


        final NodeCache cache = new NodeCache(client,nodePath);
        cache.start();

        cache.getListenable().addListener(new NodeCacheListener() {
            @Override
            public void nodeChanged() throws Exception {
                String content = new String(cache.getCurrentData().getData());
                LOG.info("stating load flow config....");
                parse(content);
            }
        });
	}
}
