package com.yomahub.liteflow.parser;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.builder.LiteFlowChainBuilder;
import com.yomahub.liteflow.builder.prop.ChainPropBean;
import com.yomahub.liteflow.builder.prop.NodePropBean;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.exception.ChainDuplicateException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.spi.holder.ContextCmpInitHolder;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.yomahub.liteflow.common.ChainConstant.ANY;
import static com.yomahub.liteflow.common.ChainConstant.CHAIN;
import static com.yomahub.liteflow.common.ChainConstant.ERROR_RESUME;
import static com.yomahub.liteflow.common.ChainConstant.FILE;
import static com.yomahub.liteflow.common.ChainConstant.GROUP;
import static com.yomahub.liteflow.common.ChainConstant.ID;
import static com.yomahub.liteflow.common.ChainConstant.NAME;
import static com.yomahub.liteflow.common.ChainConstant.NODE;
import static com.yomahub.liteflow.common.ChainConstant.NODES;
import static com.yomahub.liteflow.common.ChainConstant.THREAD_EXECUTOR_CLASS;
import static com.yomahub.liteflow.common.ChainConstant.TYPE;
import static com.yomahub.liteflow.common.ChainConstant.VALUE;
import static com.yomahub.liteflow.common.ChainConstant._CLASS;

;

/**
 * xml形式的解析器
 *
 * @author Bryan.Zhang
 */
public abstract class XmlFlowParser extends BaseFlowParser {

    private final Logger LOG = LoggerFactory.getLogger(XmlFlowParser.class);

    private final Set<String> CHAIN_NAME_SET = new CopyOnWriteArraySet<>();

    public void parse(String content) throws Exception {
        parse(ListUtil.toList(content));
    }

    @Override
    public void parse(List<String> contentList) throws Exception {
        if (CollectionUtil.isEmpty(contentList)) {
            return;
        }
        List<Document> documentList = ListUtil.toList();
        for (String content : contentList) {
            Document document = DocumentHelper.parseText(content);
            documentList.add(document);
        }
        parseDocument(documentList);
    }

    //xml形式的主要解析过程
    public void parseDocument(List<Document> documentList) throws Exception {
        //在相应的环境下进行节点的初始化工作
        //在spring体系下会获得spring扫描后的节点，接入元数据
        //在非spring体系下是一个空实现，等于不做此步骤
        ContextCmpInitHolder.loadContextCmpInit().initCmp();

        //先在元数据里放上chain
        //先放有一个好处，可以在parse的时候先映射到FlowBus的chainMap，然后再去解析
        //这样就不用去像之前的版本那样回归调用
        //同时也解决了不能循环依赖的问题
        documentList.forEach(document -> {
            // 解析chain节点
            List<Element> chainList = document.getRootElement().elements(CHAIN);

            //先在元数据里放上chain
            chainList.forEach(e -> {

                // 校验加载的 chainName 是否有重复的
                String chainName = e.attributeValue(NAME);
                if (!CHAIN_NAME_SET.add(chainName)) {
                    throw new ChainDuplicateException(String.format("[chain name duplicate] chainName=%s", chainName));
                }

                FlowBus.addChain(chainName);
            });
        });
        // 清空
        CHAIN_NAME_SET.clear();

        for (Document document : documentList) {
            Element rootElement = document.getRootElement();
            Element nodesElement = rootElement.element(NODES);
            // 当存在<nodes>节点定义时，解析node节点
            if (ObjectUtil.isNotNull(nodesElement)) {
                List<Element> nodeList = nodesElement.elements(NODE);
                String id, name, clazz, type, script, file;
                for (Element e : nodeList) {
                    id = e.attributeValue(ID);
                    name = e.attributeValue(NAME);
                    clazz = e.attributeValue(_CLASS);
                    type = e.attributeValue(TYPE);
                    script = e.getTextTrim();
                    file = e.attributeValue(FILE);

                    // 构建 node
                    NodePropBean nodePropBean = new NodePropBean()
                            .setId(id)
                            .setName(name)
                            .setClazz(clazz)
                            .setScript(script)
                            .setType(type)
                            .setFile(file);

                    buildNode(nodePropBean);
                }
            }

            //解析每一个chain
            List<Element> chainList = rootElement.elements(CHAIN);
            chainList.forEach(this::parseOneChain);
        }
    }

    /**
     * 解析一个chain的过程
     */
    private void parseOneChain(Element e) {
        String condValueStr;
        String group;
        String errorResume;
        String any;
        String threadExecutorClass;
        ConditionTypeEnum conditionType;

        //构建chainBuilder
        String chainName = e.attributeValue(NAME);
        LiteFlowChainBuilder chainBuilder = LiteFlowChainBuilder.createChain().setChainName(chainName);

        for (Iterator<Element> it = e.elementIterator(); it.hasNext(); ) {
            Element condE = it.next();
            conditionType = ConditionTypeEnum.getEnumByCode(condE.getName());
            condValueStr = condE.attributeValue(VALUE);
            errorResume = condE.attributeValue(ERROR_RESUME);
            group = condE.attributeValue(GROUP);
            any = condE.attributeValue(ANY);
            threadExecutorClass = condE.attributeValue(THREAD_EXECUTOR_CLASS);

            ChainPropBean chainPropBean = new ChainPropBean()
                    .setCondValueStr(condValueStr)
                    .setGroup(group)
                    .setErrorResume(errorResume)
                    .setAny(any)
                    .setThreadExecutorClass(threadExecutorClass)
                    .setConditionType(conditionType);

            // 构建 chain
            buildChain(chainPropBean, chainBuilder);
        }
    }

}
