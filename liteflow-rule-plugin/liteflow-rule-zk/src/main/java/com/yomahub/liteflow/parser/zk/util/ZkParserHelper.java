package com.yomahub.liteflow.parser.zk.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.helper.NodeConvertHelper;
import com.yomahub.liteflow.parser.zk.exception.ZkException;
import com.yomahub.liteflow.parser.zk.vo.ZkParserVO;
import com.yomahub.liteflow.util.RuleParsePluginUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
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

    private final String NODE_XML_PATTERN = "<nodes>{}</nodes>";

    private final String XML_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow>{}{}</flow>";

    public ZkParserHelper(ZkParserVO zkParserVO) {
        this.zkParserVO = zkParserVO;

        try {
            CuratorFramework client = CuratorFrameworkFactory.newClient(zkParserVO.getConnectStr(),
                    new RetryNTimes(10, 5000));
            client.start();

            this.client = client;
        } catch (Exception e) {
            throw new ZkException(e);
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
            // 获取chainPath路径下的所有子节点内容List
            List<String> chainItemContentList = new ArrayList<>();
            for (String chainName : chainNameList) {
                RuleParsePluginUtil.ChainDto chainDto = RuleParsePluginUtil.parseChainKey(chainName);
                String chainData = new String(
                        client.getData().forPath(StrUtil.format("{}/{}", zkParserVO.getChainPath(), chainName)));
                if (StrUtil.isNotBlank(chainData)) {
                    chainItemContentList.add(chainDto.toElXml(chainData));
                }
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

                    nodeSimpleVO.setScript(scriptData);
                    scriptItemContentList.add(RuleParsePluginUtil.toScriptXml(nodeSimpleVO));
                }

                scriptAllContent = StrUtil.format(NODE_XML_PATTERN,
                        CollUtil.join(scriptItemContentList, StrUtil.EMPTY));
            }

            return StrUtil.format(XML_PATTERN, scriptAllContent, chainAllContent);
        } catch (Exception e) {
            throw new ZkException(e);
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
        } catch (Exception e) {
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
            ChildData currChildData = data == null? oldData : data;
            String path = currChildData.getPath();
            String value = new String(currChildData.getData());
            if (StrUtil.isBlank(value)) {
                return;
            }
            if (ListUtil.toList(CuratorCacheListener.Type.NODE_CREATED, CuratorCacheListener.Type.NODE_CHANGED)
                    .contains(type)) {
                LOG.info("starting reload flow config... {} path={} value={},", type.name(), path, value);
                String chainName = FileNameUtil.getName(path);
                Pair<Boolean/*启停*/, String/*id*/> pair = RuleParsePluginUtil.parseIdKey(chainName);
                String id = pair.getValue();
                // 如果是启用，就正常更新
                if (pair.getKey()) {
                    LiteFlowChainELBuilder.createChain().setChainId(id).setEL(value).build();
                }
                // 如果是禁用，就删除
                else {
                    FlowBus.removeChain(id);
                }
            } else if (CuratorCacheListener.Type.NODE_DELETED.equals(type)) {
                LOG.info("starting reload flow config... delete path={}", path);
                String chainName = FileNameUtil.getName(path);
                Pair<Boolean/*启停*/, String/*id*/> pair = RuleParsePluginUtil.parseIdKey(chainName);
                FlowBus.removeChain(pair.getValue());
            }
        });

        if (StrUtil.isNotBlank(zkParserVO.getScriptPath())) {
            // 监听script
            CuratorCache cache2 = CuratorCache.build(client, zkParserVO.getScriptPath());
            cache2.start();
            cache2.listenable().addListener((type, oldData, data) -> {
                ChildData currChildData = data == null? oldData : data;
                String path = currChildData.getPath();
                String value = new String(currChildData.getData());
                if (StrUtil.isBlank(value)) {
                    return;
                }
                if (ListUtil.toList(CuratorCacheListener.Type.NODE_CREATED, CuratorCacheListener.Type.NODE_CHANGED)
                        .contains(type)) {
                    LOG.info("starting reload flow config... {} path={} value={},", type.name(), path, value);
                    String scriptNodeValue = FileNameUtil.getName(path);
                    NodeConvertHelper.NodeSimpleVO nodeSimpleVO = NodeConvertHelper.convert(scriptNodeValue);

                    // 启用就正常更新
                    if (nodeSimpleVO.getEnable()) {
                        LiteFlowNodeBuilder.createScriptNode()
                                .setId(nodeSimpleVO.getNodeId())
                                .setType(NodeTypeEnum.getEnumByCode(nodeSimpleVO.getType()))
                                .setName(nodeSimpleVO.getName())
                                .setScript(value)
                                .setLanguage(nodeSimpleVO.getLanguage())
                                .build();
                    }
                    // 禁用就删除
                    else {
                        FlowBus.unloadScriptNode(nodeSimpleVO.getNodeId());
                    }
                } else if (CuratorCacheListener.Type.NODE_DELETED.equals(type)) {
                    LOG.info("starting reload flow config... delete path={}", path);
                    String scriptNodeValue = FileNameUtil.getName(path);
                    NodeConvertHelper.NodeSimpleVO nodeSimpleVO = NodeConvertHelper.convert(scriptNodeValue);
                    FlowBus.unloadScriptNode(nodeSimpleVO.getNodeId());
                }
            });
        }
    }
}
