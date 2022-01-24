package com.yomahub.liteflow.parser;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.LiteFlowChainBuilder;
import com.yomahub.liteflow.builder.LiteFlowConditionBuilder;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.ConditionTypeEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.*;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.spring.ComponentScanner;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * xml形式的解析器
 * @author Bryan.Zhang
 */
public abstract class XmlFlowParser extends FlowParser {

    private final Logger LOG = LoggerFactory.getLogger(XmlFlowParser.class);

    public void parse(String content) throws Exception {
        parse(ListUtil.toList(content));
    }

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
        //先进行Spring上下文中的节点的判断
        for (Entry<String, NodeComponent> componentEntry : ComponentScanner.nodeComponentMap.entrySet()) {
            if (!FlowBus.containNode(componentEntry.getKey())) {
                FlowBus.addSpringScanNode(componentEntry.getKey(), componentEntry.getValue());
            }
        }

        //先在元数据里放上chain
        //先放有一个好处，可以在parse的时候先映射到FlowBus的chainMap，然后再去解析
        //这样就不用去像之前的版本那样回归调用
        //同时也解决了不能循环依赖的问题
        documentList.forEach(document -> {
            // 解析chain节点
            List<Element> chainList = document.getRootElement().elements("chain");

            //先在元数据里放上chain
            chainList.forEach(e -> FlowBus.addChain(e.attributeValue("name")));
        });

        for (Document document : documentList) {
            Element rootElement = document.getRootElement();
            Element nodesElement = rootElement.element("nodes");
            // 当存在<nodes>节点定义时，解析node节点
            if (ObjectUtil.isNotNull(nodesElement)){
                List<Element> nodeList = nodesElement.elements("node");
                String id, name, clazz, type, script, file;
                for (Element e : nodeList) {
                    id = e.attributeValue("id");
                    name = e.attributeValue("name");
                    clazz = e.attributeValue("class");
                    type = e.attributeValue("type");
                    script = e.getTextTrim();
                    file = e.attributeValue("file");

                    //初始化type
                    if (StrUtil.isBlank(type)){
                        type = NodeTypeEnum.COMMON.getCode();
                    }

                    //检查nodeType是不是规定的类型
                    NodeTypeEnum nodeTypeEnum = NodeTypeEnum.getEnumByCode(type);
                    if (ObjectUtil.isNull(nodeTypeEnum)){
                        throw new NodeTypeNotSupportException(StrUtil.format("type [{}] is not support", type));
                    }

                    //进行node的build过程
                    LiteFlowNodeBuilder.createNode().setId(id)
                            .setName(name)
                            .setClazz(clazz)
                            .setType(nodeTypeEnum)
                            .setScript(script)
                            .setFile(file)
                            .build();
                }
            }

            //解析每一个chain
            List<Element> chainList = rootElement.elements("chain");
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
        String chainName = e.attributeValue("name");
        LiteFlowChainBuilder chainBuilder = LiteFlowChainBuilder.createChain().setChainName(chainName);

        for (Iterator<Element> it = e.elementIterator(); it.hasNext(); ) {
            Element condE = it.next();
            conditionType = ConditionTypeEnum.getEnumByCode(condE.getName());
            condValueStr = condE.attributeValue("value");
            errorResume = condE.attributeValue("errorResume");
            group = condE.attributeValue("group");
            any = condE.attributeValue("any");
            threadExecutorClass = condE.attributeValue("threadExecutorClass");

            if (ObjectUtil.isNull(conditionType)){
                throw new NotSupportConditionException("ConditionType is not supported");
            }

            if (StrUtil.isBlank(condValueStr)) {
                throw new EmptyConditionValueException("Condition value cannot be empty");
            }

            //如果是when类型的话，有特殊化参数要设置，只针对于when的
            if (conditionType.equals(ConditionTypeEnum.TYPE_WHEN)){
                chainBuilder.setCondition(
                        LiteFlowConditionBuilder.createWhenCondition()
                                .setErrorResume(errorResume)
                                .setGroup(group)
                                .setAny(any)
                                .setThreadExecutorClass(threadExecutorClass)
                                .setValue(condValueStr)
                                .build()
                ).build();
            }else{
                chainBuilder.setCondition(
                        LiteFlowConditionBuilder.createCondition(conditionType)
                                .setValue(condValueStr)
                                .build()
                ).build();
            }
        }
    }
}
