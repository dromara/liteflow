package com.yomahub.liteflow.parser.etcd.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.etcd.EtcdClient;
import com.yomahub.liteflow.parser.etcd.exception.EtcdException;
import com.yomahub.liteflow.parser.etcd.vo.EtcdParserVO;
import com.yomahub.liteflow.parser.helper.NodeConvertHelper;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.util.RuleParsePluginUtil;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author zendwang
 * @since 2.9.0
 */
public class EtcdParserHelper {

    private static final Logger LOG = LoggerFactory.getLogger(EtcdParserHelper.class);

    private final String NODE_XML_PATTERN = "<nodes>{}</nodes>";

    private final String XML_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow>{}{}</flow>";

    private static final String SEPARATOR = "/";

    private final EtcdParserVO etcdParserVO;

    private EtcdClient client;

    public EtcdParserHelper(EtcdParserVO etcdParserVO) {
        this.etcdParserVO = etcdParserVO;

        try {
            try {
                this.client = ContextAwareHolder.loadContextAware().getBean(EtcdClient.class);
            } catch (Exception ignored) {
            }
            if (this.client == null) {
                ClientBuilder clientBuilder = Client.builder().endpoints(etcdParserVO.getEndpoints().split(","));
                if (StrUtil.isNotBlank(etcdParserVO.getNamespace())) {
                    clientBuilder.namespace(ByteSequence.from(etcdParserVO.getNamespace(), CharsetUtil.CHARSET_UTF_8));
                }
                if (StrUtil.isAllNotBlank(etcdParserVO.getUser(), etcdParserVO.getPassword())) {
                    clientBuilder.user(ByteSequence.from(etcdParserVO.getUser(), CharsetUtil.CHARSET_UTF_8));
                    clientBuilder.password(ByteSequence.from(etcdParserVO.getPassword(), CharsetUtil.CHARSET_UTF_8));
                }
                this.client = new EtcdClient(clientBuilder.build());
            }
        } catch (Exception e) {
            throw new EtcdException(e);
        }
    }

    public String getContent() {
        try {
            // 检查chainPath路径下有没有子节点
            List<String> chainNameList = client.getChildrenKeys(etcdParserVO.getChainPath(), SEPARATOR);

            // 获取chainPath路径下的所有子节点内容List
            List<String> chainItemContentList = new ArrayList<>();
            for (String chainName : chainNameList) {
                RuleParsePluginUtil.ChainDto chainDto = RuleParsePluginUtil.parseChainKey(chainName);
                String chainData = client.get(StrUtil.format("{}/{}", etcdParserVO.getChainPath(), chainName));
                if (StrUtil.isNotBlank(chainData)) {
                    chainItemContentList.add(chainDto.toElXml(chainData));
                }
            }
            // 合并成所有chain的xml内容
            String chainAllContent = CollUtil.join(chainItemContentList, StrUtil.EMPTY);

            // 检查是否有脚本内容，如果有，进行脚本内容的获取
            String scriptAllContent = StrUtil.EMPTY;
            if (hasScript()) {
                List<String> scriptNodeValueList = client.getChildrenKeys(etcdParserVO.getScriptPath(), SEPARATOR)
                        .stream()
                        .filter(StrUtil::isNotBlank)
                        .collect(Collectors.toList());

                List<String> scriptItemContentList = new ArrayList<>();
                for (String scriptNodeValue : scriptNodeValueList) {
                    NodeConvertHelper.NodeSimpleVO nodeSimpleVO = NodeConvertHelper.convert(scriptNodeValue);
                    if (Objects.isNull(nodeSimpleVO)) {
                        throw new EtcdException(
                                StrUtil.format("The name of the etcd node is invalid:{}", scriptNodeValue));
                    }
                    String scriptData = client
                            .get(StrUtil.format("{}/{}", etcdParserVO.getScriptPath(), scriptNodeValue));

                    nodeSimpleVO.setScript(scriptData);
                    scriptItemContentList.add(RuleParsePluginUtil.toScriptXml(nodeSimpleVO));
                }


                scriptAllContent = StrUtil.format(NODE_XML_PATTERN,
                        CollUtil.join(scriptItemContentList, StrUtil.EMPTY));
            }

            return StrUtil.format(XML_PATTERN, scriptAllContent, chainAllContent);
        } catch (Exception e) {
            throw new EtcdException(e);
        }
    }

    public boolean hasScript() {
        // 没有配置scriptPath
        if (StrUtil.isBlank(etcdParserVO.getScriptPath())) {
            return false;
        }

        try {
            // 存在这个节点，但是子节点不存在
            List<String> chainNameList = client.getChildrenKeys(etcdParserVO.getScriptPath(), SEPARATOR);
            return !CollUtil.isEmpty(chainNameList);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 监听 etcd 节点
     */
    public void listen() {
        this.client.watchChildChange(this.etcdParserVO.getChainPath(), (updatePath, updateValue) -> {
            LOG.info("starting reload flow config... update path={} value={},", updatePath, updateValue);
            String changeKey = FileNameUtil.getName(updatePath);
            Pair<Boolean/*启停*/, String/*id*/> pair = RuleParsePluginUtil.parseIdKey(changeKey);
            String chainId = pair.getValue();
            // 如果是启用，就正常更新
            if (pair.getKey()) {
                LiteFlowChainELBuilder.createChain().setChainId(chainId).setEL(updateValue).build();
            }
            // 如果是禁用，就删除
            else {
                FlowBus.removeChain(chainId);
            }
        }, (deletePath) -> {
            LOG.info("starting reload flow config... delete path={}", deletePath);
            String chainKey = FileNameUtil.getName(deletePath);
            Pair<Boolean/*启停*/, String/*id*/> pair = RuleParsePluginUtil.parseIdKey(chainKey);
            FlowBus.removeChain(pair.getValue());
        });

        if (StrUtil.isNotBlank(this.etcdParserVO.getScriptPath())) {
            this.client.watchChildChange(this.etcdParserVO.getScriptPath(), (updatePath, updateValue) -> {
                LOG.info("starting reload flow config... update path={} value={}", updatePath, updateValue);
                String scriptNodeValue = FileNameUtil.getName(updatePath);
                NodeConvertHelper.NodeSimpleVO nodeSimpleVO = NodeConvertHelper.convert(scriptNodeValue);
                // 启用就正常更新
                if (nodeSimpleVO.getEnable()) {
                    LiteFlowNodeBuilder.createScriptNode()
                            .setId(nodeSimpleVO.getNodeId())
                            .setType(NodeTypeEnum.getEnumByCode(nodeSimpleVO.getType()))
                            .setName(nodeSimpleVO.getName())
                            .setScript(updateValue)
                            .setLanguage(nodeSimpleVO.getLanguage())
                            .build();
                }
                // 禁用就删除
                else {
                    FlowBus.unloadScriptNode(nodeSimpleVO.getNodeId());
                }
            }, (deletePath) -> {
                LOG.info("starting reload flow config... delete path={}", deletePath);
                String scriptNodeValue = FileNameUtil.getName(deletePath);
                NodeConvertHelper.NodeSimpleVO nodeSimpleVO = NodeConvertHelper.convert(scriptNodeValue);
                FlowBus.unloadScriptNode(nodeSimpleVO.getNodeId());
            });
        }
    }


}
