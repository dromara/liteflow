package com.yomahub.liteflow.parser.helper;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.builder.prop.NodePropBean;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.*;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.condition.AbstractCondition;
import com.yomahub.liteflow.util.ElRegexUtil;
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.*;
import java.util.function.Consumer;

import static com.yomahub.liteflow.common.ChainConstant.*;

/**
 * Parser 通用 Helper
 *
 * @author tangkc
 * @author jason
 * @author zy
 */
public class ParserHelper {

    /**
     * 私有化构造器
     */
    private ParserHelper() {
    }

    /**
     * 构建 node
     * @param nodePropBean 构建 node 的中间属性
     */
    public static void buildNode(NodePropBean nodePropBean) {
        String id = nodePropBean.getId();
        String name = nodePropBean.getName();
        String clazz = nodePropBean.getClazz();
        String script = nodePropBean.getScript();
        String type = nodePropBean.getType();
        String file = nodePropBean.getFile();
        String language = nodePropBean.getLanguage();

        // clazz有值的，基本都不是脚本节点
        // 脚本节点，都必须配置type
        // 非脚本节点的先尝试自动推断类型
        if (StrUtil.isNotBlank(clazz)) {
            try {
                // 先尝试从继承的类型中推断
                Class<?> c = Class.forName(clazz);
                NodeTypeEnum nodeType = NodeTypeEnum.guessType(c);
                if (nodeType != null) {
                    type = nodeType.getCode();
                }
            }
            catch (Exception e) {
                throw new NodeClassNotFoundException(StrUtil.format("cannot find the node[{}]", clazz));
            }
        }

        // 因为脚本节点是必须设置type的，所以到这里type就全都有了，所以进行二次检查
        if (StrUtil.isBlank(type)) {
            throw new NodeTypeCanNotGuessException(StrUtil.format("cannot guess the type of node[{}]", clazz));
        }

        // 检查nodeType是不是规定的类型
        NodeTypeEnum nodeTypeEnum = NodeTypeEnum.getEnumByCode(type);
        if (ObjectUtil.isNull(nodeTypeEnum)) {
            throw new NodeTypeNotSupportException(StrUtil.format("type [{}] is not support", type));
        }

        // 进行node的build过程
        LiteFlowNodeBuilder.createNode()
                .setId(id)
                .setName(name)
                .setClazz(clazz)
                .setType(nodeTypeEnum)
                .setScript(script)
                .setFile(file)
                .setLanguage(language)
                .build();
    }

    /**
     * xml 形式的主要解析过程
     * @param documentList documentList
     */
    public static void parseNodeDocument(List<Document> documentList) {
        for (Document document : documentList) {
            Element rootElement = document.getRootElement();
            Element nodesElement = rootElement.element(NODES);
            // 当存在<nodes>节点定义时，解析node节点
            if (ObjectUtil.isNotNull(nodesElement)) {
                List<Element> nodeList = nodesElement.elements(NODE);
                String id, name, clazz, type, script, file, language;
                for (Element e : nodeList) {
                    id = e.attributeValue(ID);
                    name = e.attributeValue(NAME);
                    clazz = e.attributeValue(_CLASS);
                    type = e.attributeValue(TYPE);
                    script = e.getText();
                    file = e.attributeValue(FILE);
                    language = e.attributeValue(LANGUAGE);

                    if (!getEnableByElement(e)) {
                        continue;
                    }

                    // 构建 node
                    NodePropBean nodePropBean = new NodePropBean().setId(id)
                            .setName(name)
                            .setClazz(clazz)
                            .setScript(script)
                            .setType(type)
                            .setFile(file)
                            .setLanguage(language);

                    ParserHelper.buildNode(nodePropBean);
                }
            }
        }
    }

    public static void parseChainDocument(List<Document> documentList, Set<String> chainIdSet,
                                          Consumer<Element> parseOneChainConsumer) {
        //用于存放抽象chain的map
        Map<String,Element> abstratChainMap = new HashMap<>();
        //用于存放已经解析过的实现chain
        Set<Element> implChainSet = new HashSet<>();
        // 先在元数据里放上chain
        // 先放有一个好处，可以在parse的时候先映射到FlowBus的chainMap，然后再去解析
        // 这样就不用去像之前的版本那样回归调用
        // 同时也解决了不能循环依赖的问题
        documentList.forEach(document -> {
            // 解析chain节点
            List<Element> chainList = document.getRootElement().elements(CHAIN);

            // 先在元数据里放上chain
            for (Element e : chainList) {
                // 校验加载的 chainName 是否有重复的
                // TODO 这里是否有个问题，当混合格式加载的时候，2个同名的Chain在不同的文件里，就不行了
                String chainId = Optional.ofNullable(e.attributeValue(ID)).orElse(e.attributeValue(NAME));
                // 检查 chainName
                checkChainId(chainId, e.getText());
                if (!chainIdSet.add(chainId)) {
                    throw new ChainDuplicateException(StrUtil.format("[chain name duplicate] chainName={}", chainId));
                }
                // 如果是禁用，就不解析了
                if (!getEnableByElement(e)) {
                    continue;
                }

                FlowBus.addChain(chainId);
                if(ElRegexUtil.isAbstractChain(e.getText())){
                    abstratChainMap.put(chainId,e);
                    //如果是抽象chain，则向其中添加一个AbstractCondition,用于标记这个chain为抽象chain
                    Chain chain = FlowBus.getChain(chainId);
                    chain.getConditionList().add(new AbstractCondition());
                }
            };
        });
        // 清空
        chainIdSet.clear();

        // 解析每一个chain
        for (Document document : documentList) {
            Element rootElement = document.getRootElement();
            List<Element> chainList = rootElement.elements(CHAIN);
            for(Element chain:chainList){
                // 如果是禁用，就不解析了
                if (!getEnableByElement(chain)) {
                    continue;
                }

                //首先需要对继承自抽象Chain的chain进行字符串替换
                parseImplChain(abstratChainMap, implChainSet, chain);
                //如果一个chain不为抽象chain，则进行解析
                String chainName = Optional.ofNullable(chain.attributeValue(ID)).orElse(chain.attributeValue(NAME));
                if(!abstratChainMap.containsKey(chainName)){
                    parseOneChainConsumer.accept(chain);
                }
            }
        }
    }

    public static void parseNodeJson(List<JsonNode> flowJsonObjectList) {
        for (JsonNode flowJsonNode : flowJsonObjectList) {
            // 当存在<nodes>节点定义时，解析node节点
            if (flowJsonNode.get(FLOW).has(NODES)) {
                Iterator<JsonNode> nodeIterator = flowJsonNode.get(FLOW).get(NODES).get(NODE).elements();
                String id, name, clazz, script, type, file, language;
                while ((nodeIterator.hasNext())) {
                    JsonNode nodeObject = nodeIterator.next();
                    id = nodeObject.get(ID).textValue();
                    name = nodeObject.hasNonNull(NAME) ? nodeObject.get(NAME).textValue() : "";
                    clazz = nodeObject.hasNonNull(_CLASS) ? nodeObject.get(_CLASS).textValue() : "";
                    type = nodeObject.hasNonNull(TYPE) ? nodeObject.get(TYPE).textValue() : null;
                    script = nodeObject.hasNonNull(VALUE) ? nodeObject.get(VALUE).textValue() : "";
                    file = nodeObject.hasNonNull(FILE) ? nodeObject.get(FILE).textValue() : "";
                    language = nodeObject.hasNonNull(LANGUAGE) ? nodeObject.get(LANGUAGE).textValue() : "";

                    // 如果是禁用的，就不编译了
                    if (!getEnableByJsonNode(nodeObject)) {
                        continue;
                    }

                    // 构建 node
                    NodePropBean nodePropBean = new NodePropBean().setId(id)
                            .setName(name)
                            .setClazz(clazz)
                            .setScript(script)
                            .setType(type)
                            .setFile(file)
                            .setLanguage(language);

                    ParserHelper.buildNode(nodePropBean);
                }
            }
        }
    }

    public static void parseChainJson(List<JsonNode> flowJsonObjectList, Set<String> chainIdSet,
                                      Consumer<JsonNode> parseOneChainConsumer) {
        //用于存放抽象chain的map
        Map<String,JsonNode> abstratChainMap = new HashMap<>();
        //用于存放已经解析过的实现chain
        Set<JsonNode> implChainSet = new HashSet<>();
        // 先在元数据里放上chain
        // 先放有一个好处，可以在parse的时候先映射到FlowBus的chainMap，然后再去解析
        // 这样就不用去像之前的版本那样回归调用
        // 同时也解决了不能循环依赖的问题
        flowJsonObjectList.forEach(jsonObject -> {
            // 解析chain节点
            Iterator<JsonNode> iterator = jsonObject.get(FLOW).get(CHAIN).elements();
            // 先在元数据里放上chain
            while (iterator.hasNext()) {
                JsonNode innerJsonObject = iterator.next();
                // 校验加载的 chainName 是否有重复的
                // TODO 这里是否有个问题，当混合格式加载的时候，2个同名的Chain在不同的文件里，就不行了
                JsonNode chainNameJsonNode = Optional.ofNullable(innerJsonObject.get(ID))
                        .orElse(innerJsonObject.get(NAME));
                String chainId = Optional.ofNullable(chainNameJsonNode).map(JsonNode::textValue).orElse(null);
                // 检查 chainName
                checkChainId(chainId, innerJsonObject.toString());
                if (!chainIdSet.add(chainId)) {
                    throw new ChainDuplicateException(String.format("[chain id duplicate] chainId=%s", chainId));
                }

                // 如果是禁用，就不解析了
                if (!getEnableByJsonNode(innerJsonObject)) {
                    continue;
                }

                FlowBus.addChain(chainId);
                if(ElRegexUtil.isAbstractChain(innerJsonObject.get(VALUE).textValue())){
                    abstratChainMap.put(chainId,innerJsonObject);
                    //如果是抽象chain，则向其中添加一个AbstractCondition,用于标记这个chain为抽象chain
                    Chain chain = FlowBus.getChain(chainId);
                    chain.getConditionList().add(new AbstractCondition());
                }
            }
        });
        // 清空
        chainIdSet.clear();

        for (JsonNode flowJsonNode : flowJsonObjectList) {
            // 解析每一个chain
            Iterator<JsonNode> chainIterator = flowJsonNode.get(FLOW).get(CHAIN).elements();
            while (chainIterator.hasNext()) {
                JsonNode chainNode = chainIterator.next();
                // 如果是禁用，就不解析了
                if (!getEnableByJsonNode(chainNode)) {
                    continue;
                }

                //首先需要对继承自抽象Chain的chain进行字符串替换
                parseImplChain(abstratChainMap, implChainSet, chainNode);
                //如果一个chain不为抽象chain，则进行解析
                JsonNode chainNameJsonNode = Optional.ofNullable(chainNode.get(ID)).orElse(chainNode.get(NAME));
                String chainId = Optional.ofNullable(chainNameJsonNode).map(JsonNode::textValue).orElse(null);
                if(!abstratChainMap.containsKey(chainId)){
                    parseOneChainConsumer.accept(chainNode);
                }
            }
        }
    }

    /**
     * 解析一个chain的过程
     * @param chainNode chain 节点
     */
    public static void parseOneChainEl(JsonNode chainNode) {
        // 构建chainBuilder
        String chainId = Optional.ofNullable(chainNode.get(ID)).orElse(chainNode.get(NAME)).textValue();

        String namespace = chainNode.get(NAMESPACE) == null? DEFAULT_NAMESPACE : chainNode.get(NAMESPACE).textValue();

        JsonNode routeJsonNode = chainNode.get(ROUTE);

        String threadPoolExecutorClass = chainNode.get(THREAD_POOL_EXECUTOR_CLASS) == null ? null :
                chainNode.get(THREAD_POOL_EXECUTOR_CLASS).textValue();

        LiteFlowChainELBuilder builder =
                LiteFlowChainELBuilder.createChain().setChainId(chainId).setNamespace(namespace)
                .setThreadPoolExecutorClass(threadPoolExecutorClass);

        // 如果有route这个标签，说明是决策表chain
        // 决策表链路必须有route和body这两个标签
        if (routeJsonNode != null){
            builder.setRoute(routeJsonNode.textValue());

            JsonNode bodyJsonNode = chainNode.get(VALUE);
            if (bodyJsonNode == null){
                String errMsg = StrUtil.format("If you have defined the field route, then you must define the field body in chain[{}]", chainId);
                throw new FlowSystemException(errMsg);
            }
            builder.setEL(bodyJsonNode.textValue());
        }else{
            builder.setEL(chainNode.get(VALUE).textValue());
        }
        builder.build();
    }

    /**
     * 解析一个chain的过程
     * @param e chain 节点
     */
    public static void parseOneChainEl(Element e) {
        // 构建chainBuilder
        String chainId = Optional.ofNullable(e.attributeValue(ID)).orElse(e.attributeValue(NAME));

        String namespace = StrUtil.blankToDefault(e.attributeValue(NAMESPACE), DEFAULT_NAMESPACE);

        Element routeElement = e.element(ROUTE);

        String threadPoolExecutorClass = e.attributeValue(THREAD_POOL_EXECUTOR_CLASS) == null ? null :
                e.attributeValue(THREAD_POOL_EXECUTOR_CLASS);

        LiteFlowChainELBuilder builder =
                LiteFlowChainELBuilder.createChain().setChainId(chainId).setNamespace(namespace)
                .setThreadPoolExecutorClass(threadPoolExecutorClass);

        // 如果有route这个标签，说明是决策表chain
        // 决策表链路必须有route和body这两个标签
        if (routeElement != null){
            builder.setRoute(routeElement.getText());

            Element bodyElement = e.element(BODY);
            if (bodyElement == null){
                String errMsg = StrUtil.format("If you have defined the tag <route>, then you must define the tag <body> in chain[{}]", chainId);
                throw new FlowSystemException(errMsg);
            }
            builder.setEL(bodyElement.getText());
        }else{
            // 即使没有route这个标签，body标签单独写也是被允许的
            Element bodyElement = e.element(BODY);
            if (bodyElement != null){
                builder.setEL(bodyElement.getText());
            }else{
                builder.setEL(e.getText());
            }
        }


        builder.build();
    }

    /**
     * 检查 chainId
     * @param chainId chainId
     * @param elData elData
     */
    private static void checkChainId(String chainId, String elData) {
        if (StrUtil.isBlank(chainId)) {
            throw new ParseException("missing chain id in expression \r\n" + elData);
        }
    }

    /**
     * 解析一个带继承关系的Chain,xml格式
     * @param chain 实现Chain
     * @param abstratChainMap 所有的抽象Chain
     * @param implChainSet 已经解析过的实现Chain
     */
    private static void parseImplChain(Map<String, Element> abstratChainMap, Set<Element> implChainSet, Element chain) {
        if(ObjectUtil.isNotNull(chain.attributeValue(EXTENDS))){
            String baseChainId = chain.attributeValue(EXTENDS);
            Element baseChain = abstratChainMap.get(baseChainId);
            if(baseChain!=null) {
                internalParseImplChain(baseChain,chain,abstratChainMap,implChainSet);
            }else{
                throw new ChainNotFoundException(StrUtil.format("[abstract chain not found] chainName={}", baseChainId));
            }
        }
    }

    /**
     * 解析一个带继承关系的Chain,json格式
     * @param chainNode 实现Chain
     * @param abstratChainMap 所有的抽象Chain
     * @param implChainSet 已经解析过的实现Chain
     */
    private static void parseImplChain(Map<String, JsonNode> abstratChainMap, Set<JsonNode> implChainSet, JsonNode chainNode) {
        if(chainNode.hasNonNull(EXTENDS)){
            String baseChainId = chainNode.get(EXTENDS).textValue();
            JsonNode baseChain= abstratChainMap.get(baseChainId);
            if(baseChain!=null) {
                internalParseImplChain(baseChain,chainNode,abstratChainMap,implChainSet);
            }else{
                throw new ChainNotFoundException(StrUtil.format("[abstract chain not found] chainName={}", baseChainId));
            }
        }
    }

    /**
     * 解析一个继承自baseChain的implChain,xml格式
     * @param baseChain 父Chain
     * @param implChain 实现Chain
     * @param abstractChainMap 所有的抽象Chain
     * @param implChainSet 已经解析过的实现Chain
     */
    private static void internalParseImplChain(JsonNode baseChain,JsonNode implChain,Map<String,JsonNode> abstractChainMap,Set<JsonNode> implChainSet) {
        //如果已经解析过了，就不再解析
        if(implChainSet.contains(implChain)) return;
        //如果baseChainId也是继承自其他的chain，需要递归解析
        parseImplChain(abstractChainMap, implChainSet, baseChain);
        //否则根据baseChainId解析implChainId
        String implChainEl = implChain.get(VALUE).textValue();
        String baseChainEl = baseChain.get(VALUE).textValue();
        //替换baseChainId中的implChainId
        // 使用正则表达式匹配占位符并替换
        String parsedEl = ElRegexUtil.replaceAbstractChain(baseChainEl,implChainEl);
        ObjectNode objectNode = (ObjectNode) implChain;
        objectNode.put(VALUE,parsedEl);
        implChainSet.add(implChain);
    }

    /**
     * 解析一个继承自baseChain的implChain,json格式
     * @param baseChain 父Chain
     * @param implChain 实现Chain
     * @param abstractChainMap 所有的抽象Chain
     * @param implChainSet 已经解析过的实现Chain
     */
    private static void internalParseImplChain(Element baseChain,Element implChain,Map<String,Element> abstractChainMap,Set<Element> implChainSet) {
        //如果已经解析过了，就不再解析
        if(implChainSet.contains(implChain)) return;
        //如果baseChainId也是继承自其他的chain，需要递归解析
        parseImplChain(abstractChainMap, implChainSet, baseChain);
        //否则根据baseChainId解析implChainId
        String implChainEl = implChain.getText();
        String baseChainEl = baseChain.getText();
        //替换baseChainId中的implChainId
        // 使用正则表达式匹配占位符并替换
        String parsedEl = ElRegexUtil.replaceAbstractChain(baseChainEl,implChainEl);
        implChain.setText(parsedEl);
        implChainSet.add(implChain);
    }

    private static Boolean getEnableByElement(Element element) {
        String enableStr = element.attributeValue(ENABLE);
        if (StrUtil.isBlank(enableStr)) {
            return true;
        }
        return Boolean.TRUE.toString().equalsIgnoreCase(enableStr);
    }

    private static Boolean getEnableByJsonNode(JsonNode nodeObject) {
        String enableStr = nodeObject.hasNonNull(ENABLE) ? nodeObject.get(ENABLE).toString() : "";
        if (StrUtil.isBlank(enableStr)) {
            return true;
        }
        return Boolean.TRUE.toString().equalsIgnoreCase(enableStr);
    }
}
