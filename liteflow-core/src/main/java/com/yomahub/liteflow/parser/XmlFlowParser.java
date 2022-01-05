package com.yomahub.liteflow.parser;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.common.LocalDefaultFlowConstant;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.flow.Chain;
import com.yomahub.liteflow.entity.flow.Condition;
import com.yomahub.liteflow.entity.flow.Executable;
import com.yomahub.liteflow.entity.flow.Node;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.CyclicDependencyException;
import com.yomahub.liteflow.exception.ExecutableItemNotFoundException;
import com.yomahub.liteflow.exception.NodeTypeNotSupportException;
import com.yomahub.liteflow.exception.ParseException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.spring.ComponentScanner;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * xml形式的解析器
 *
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
                    file = e.attributeValue("file");

                    //初始化type
                    if (StrUtil.isBlank(type)){
                        type = NodeTypeEnum.COMMON.getCode();
                    }
                    NodeTypeEnum nodeTypeEnum = NodeTypeEnum.getEnumByCode(type);
                    if (ObjectUtil.isNull(nodeTypeEnum)){
                        throw new NodeTypeNotSupportException(StrUtil.format("type [{}] is not support", type));
                    }

                    //这里区分是普通java节点还是脚本节点
                    //如果是脚本节点，又区分是普通脚本节点，还是条件脚本节点
                    if (nodeTypeEnum.equals(NodeTypeEnum.COMMON) && StrUtil.isNotBlank(clazz)){
                        FlowBus.addCommonNode(id, name, clazz);
                    }else if(nodeTypeEnum.equals(NodeTypeEnum.SCRIPT) || nodeTypeEnum.equals(NodeTypeEnum.COND_SCRIPT)){
                        //如果file字段不为空，则优先从file里面读取脚本文本
                        if (StrUtil.isNotBlank(file)){
                            script = ResourceUtil.readUtf8Str(StrUtil.format("classpath: {}", file));
                        }else{
                            script = e.getTextTrim();
                        }

                        //根据节点类型把脚本添加到元数据里
                        if (nodeTypeEnum.equals(NodeTypeEnum.SCRIPT)){
                            FlowBus.addCommonScriptNode(id, name, script);
                        }else {
                            FlowBus.addCondScriptNode(id, name, script);
                        }
                    }
                }
            }

            // 解析chain节点
            List<Element> chainList = rootElement.elements("chain");
            for (Element e : chainList) {
                parseOneChain(e, documentList);
            }
        }
    }

    /**
     * 解析一个chain的过程
     */
    private void parseOneChain(Element e, List<Document> documentList) throws Exception {
        String condArrayStr;
        String[] condArray;
        String group;
        String errorResume;
        String any;
        Condition condition;
        Element condE;
        List<Executable> chainNodeList;
        List<Condition> conditionList = new ArrayList<>();

        String chainName = e.attributeValue("name");
        for (Iterator<Element> it = e.elementIterator(); it.hasNext(); ) {
            condE = it.next();
            condArrayStr = condE.attributeValue("value");
            errorResume = condE.attributeValue("errorResume");
            group = condE.attributeValue("group");
            any = condE.attributeValue("any");
            if (StrUtil.isBlank(condArrayStr)) {
                continue;
            }
            if (StrUtil.isBlank(group)) {
                group = LocalDefaultFlowConstant.DEFAULT;
            }
            if (StrUtil.isBlank(errorResume)) {
                errorResume = Boolean.FALSE.toString();
            }
            if (StrUtil.isBlank(any)){
                any = Boolean.FALSE.toString();
            }
            condition = new Condition();
            chainNodeList = new ArrayList<>();
            condArray = condArrayStr.split(",");
            RegexEntity regexEntity;
            String itemExpression;
            RegexNodeEntity item;
            //这里解析的规则，优先按照node去解析，再按照chain去解析
            for (String s : condArray) {
                itemExpression = s.trim();
                regexEntity = RegexEntity.parse(itemExpression);
                item = regexEntity.getItem();
                if (FlowBus.containNode(item.getId())) {
                    Node node = FlowBus.copyNode(item.getId());
                    node.setTag(regexEntity.getItem().getTag());
                    chainNodeList.add(node);
                    //这里判断是不是条件节点，条件节点会含有realItem，也就是括号里的node
                    if (regexEntity.getRealItemArray() != null) {
                        for (RegexNodeEntity realItem : regexEntity.getRealItemArray()) {
                            if (FlowBus.containNode(realItem.getId())) {
                                Node condNode = FlowBus.copyNode(realItem.getId());
                                condNode.setTag(realItem.getTag());
                                node.setCondNode(condNode.getId(), condNode);
                            } else if (hasChain(documentList, realItem.getId())) {
                                Chain chain = FlowBus.getChain(realItem.getId());
                                node.setCondNode(chain.getChainName(), chain);
                            } else{
                                String errorMsg = StrUtil.format("executable node[{}] is not found!", realItem.getId());
                                throw new ExecutableItemNotFoundException(errorMsg);
                            }
                        }
                    }
                } else if (hasChain(documentList, item.getId())) {
                    Chain chain = FlowBus.getChain(item.getId());
                    chainNodeList.add(chain);
                } else {
                    String errorMsg = StrUtil.format("executable node[{}] is not found!", regexEntity.getItem().getId());
                    throw new ExecutableItemNotFoundException(errorMsg);
                }
            }
            condition.setErrorResume(Boolean.parseBoolean(errorResume));
            condition.setGroup(group);
            condition.setAny(any.equals(Boolean.TRUE.toString()));
            condition.setConditionType(condE.getName());
            condition.setNodeList(chainNodeList);

            //这里把condition组装进conditionList，根据参数有些condition要和conditionList里面的某些进行合并操作
            super.buildConditions(conditionList, condition);
        }
        FlowBus.addChain(chainName, new Chain(chainName, conditionList));
    }

    //判断在这个FlowBus元数据里是否含有这个chain
    //因为chain和node都是可执行器，在一个规则文件上，有可能是node，有可能是chain
    @SuppressWarnings("unchecked")
    private boolean hasChain(List<Document> documentList, String chainName) throws Exception {
        try{
            for (Document document : documentList) {
                List<Element> chainList = document.getRootElement().elements("chain");
                for (Element ce : chainList) {
                    String ceName = ce.attributeValue("name");
                    if (ceName.equals(chainName)) {
                        if (!FlowBus.containChain(chainName)) {
                            parseOneChain(ce, documentList);
                        }
                        return true;
                    }
                }
            }
            return false;
        }catch (StackOverflowError e){
            LOG.error("a cyclic dependency occurs in chain", e);
            throw new CyclicDependencyException("a cyclic dependency occurs in chain");
        }
    }
}
