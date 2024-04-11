package com.yomahub.liteflow.parser.apollo.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.apollo.exception.ApolloException;
import com.yomahub.liteflow.parser.apollo.vo.ApolloParserConfigVO;
import com.yomahub.liteflow.parser.helper.NodeConvertHelper;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.util.RuleParsePluginUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ctrip.framework.apollo.enums.PropertyChangeType.DELETED;

/**
 * @author zhanghua
 * @since 2.9.5
 */
public class ApolloParseHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ApolloParseHelper.class);

    private final String NODE_XML_PATTERN = "<nodes>{}</nodes>";

    private final String XML_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow>{}{}</flow>";

    private final ApolloParserConfigVO apolloParserConfigVO;

    private Config chainConfig;

    private Config scriptConfig;

    public ApolloParseHelper(ApolloParserConfigVO apolloParserConfigVO) {
        this.apolloParserConfigVO = apolloParserConfigVO;

        try {
            try {
                // 这里本身对于程序运行来说没有什么意义，拿到的永远是null
                // 其实config对象也没有注入到spring容器中
                // 这里这样写的目的是为了单测中的mockito，当有@MockBean的时候，这里就能拿到了
                this.chainConfig = ContextAwareHolder.loadContextAware().getBean("chainConfig");
                this.scriptConfig = ContextAwareHolder.loadContextAware().getBean("scriptConfig");
            } catch (Exception ignored) {
            }

            if (ObjectUtil.isNull(chainConfig)) {
                chainConfig = ConfigService.getConfig(apolloParserConfigVO.getChainNamespace());
                String scriptNamespace;
                // scriptConfig is optional
                if (StrUtil.isNotBlank(scriptNamespace = apolloParserConfigVO.getScriptNamespace())) {
                    scriptConfig = ConfigService.getConfig(scriptNamespace);
                }
            }
        } catch (Exception e) {
            throw new ApolloException(e.getMessage());
        }
    }

    public String getContent() {

        try {
            // 1. handle chain
            Set<String> propertyNames = chainConfig.getPropertyNames();
            List<String> chainItemContentList = propertyNames.stream()
                    .map(item -> RuleParsePluginUtil.parseChainKey(item).toElXml(chainConfig.getProperty(item, StrUtil.EMPTY)))
                    .collect(Collectors.toList());
            // merge all chain content
            String chainAllContent = CollUtil.join(chainItemContentList, StrUtil.EMPTY);

            // 2. handle script if needed
            String scriptAllContent = StrUtil.EMPTY;
            Set<String> scriptNamespaces;
            if (Objects.nonNull(scriptConfig)
                    && CollectionUtil.isNotEmpty(scriptNamespaces = scriptConfig.getPropertyNames())) {

                List<String> scriptItemContentList = scriptNamespaces.stream()
                        .map(item -> convert(item, scriptConfig.getProperty(item, StrUtil.EMPTY)))
                        .map(RuleParsePluginUtil::toScriptXml)
                        .collect(Collectors.toList());

                scriptAllContent = StrUtil.format(NODE_XML_PATTERN,
                        CollUtil.join(scriptItemContentList, StrUtil.EMPTY));
            }

            return StrUtil.format(XML_PATTERN, scriptAllContent, chainAllContent);
        } catch (Exception e) {
            throw new ApolloException(e.getMessage());
        }
    }

    /**
     * listen apollo config change
     */
    public void listenApollo() {
        // chain
        chainConfig.addChangeListener(changeEvent -> changeEvent.changedKeys().forEach(changeKey -> {
            ConfigChange configChange = changeEvent.getChange(changeKey);
            String newValue = configChange.getNewValue();
            PropertyChangeType changeType = configChange.getChangeType();
            Pair<Boolean/*启停*/, String/*id*/> pair = RuleParsePluginUtil.parseIdKey(changeKey);
            String chainId = pair.getValue();
            switch (changeType) {
                case ADDED:
                case MODIFIED:
                    LOG.info("starting reload flow config... {} key={} value={},", changeType.name(), changeKey,
                            newValue);
                    // 如果是启用，就正常更新
                    if (pair.getKey()) {
                        LiteFlowChainELBuilder.createChain().setChainId(chainId).setEL(newValue).build();
                    }
                    // 如果是禁用，就删除
                    else {
                        FlowBus.removeChain(chainId);
                    }
                    break;
                case DELETED:
                    LOG.info("starting reload flow config... delete key={}", changeKey);
                    FlowBus.removeChain(chainId);
                    break;
                default:
            }
        }));

        if (StrUtil.isNotBlank(apolloParserConfigVO.getScriptNamespace())) {
            scriptConfig.addChangeListener(changeEvent -> changeEvent.changedKeys().forEach(changeKey -> {
                ConfigChange configChange = changeEvent.getChange(changeKey);
                String newValue = configChange.getNewValue();

                PropertyChangeType changeType = configChange.getChangeType();
                if (DELETED.equals(changeType)) {
                    newValue = null;
                }
                NodeConvertHelper.NodeSimpleVO nodeSimpleVO = convert(changeKey, newValue);
                switch (changeType) {
                    case ADDED:
                    case MODIFIED:
                        LOG.info("starting reload flow config... {} key={} value={},", changeType.name(), changeKey,
                                newValue);

                        // 启用就正常更新
                        if (nodeSimpleVO.getEnable()) {
                            LiteFlowNodeBuilder.createScriptNode()
                                    .setId(nodeSimpleVO.getNodeId())
                                    .setType(NodeTypeEnum.getEnumByCode(nodeSimpleVO.getType()))
                                    .setName(nodeSimpleVO.getName())
                                    .setScript(newValue)
                                    .setLanguage(nodeSimpleVO.getLanguage())
                                    .build();
                        }
                        // 禁用就删除
                        else {
                            FlowBus.unloadScriptNode(nodeSimpleVO.getNodeId());
                        }
                        break;
                    case DELETED:
                        LOG.info("starting reload flow config... delete key={}", changeKey);
                        FlowBus.unloadScriptNode(nodeSimpleVO.getNodeId());
                        break;
                    default:
                }
            }));
        }
    }

    private NodeConvertHelper.NodeSimpleVO convert(String key, String value) {
        NodeConvertHelper.NodeSimpleVO nodeSimpleVO = NodeConvertHelper.convert(key);
        // set script
        nodeSimpleVO.setScript(value);

        return nodeSimpleVO;
    }


}
