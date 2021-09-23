package com.yomahub.liteflow.parser;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.yomahub.liteflow.exception.ParseException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;

/**
 * 基于zk方式的yml形式的解析器
 * @author guodongqing
 * @since 2.5.0
 */
public class ZookeeperYmlFlowParser extends YmlFlowParser{

    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperYmlFlowParser.class);

    private final String nodePath;

    public ZookeeperYmlFlowParser(String node) {
        nodePath = node;
    }

    @Override
    public void parseMain(List<String> pathList) throws Exception {
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

        String content = new String(client.getData().forPath(nodePath));

        if (StrUtil.isBlank(content)) {
            String error = MessageFormat.format("the node[{0}] value is empty", nodePath);
            throw new ParseException(error);
        }

        JSONObject ruleObject = convertToJson(content);

        parse(ruleObject.toJSONString());


        final NodeCache cache = new NodeCache(client,nodePath);
        cache.start();

        cache.getListenable().addListener(() -> {
            String content1 = new String(cache.getCurrentData().getData());
            LOG.info("stating load flow config....");
            JSONObject ruleObject1 = convertToJson(content1);
            parse(ruleObject1.toJSONString());
        });
    }
}
