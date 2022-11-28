/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.flow;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.*;
import com.yomahub.liteflow.enums.FlowParserTypeEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.ComponentCannotRegisterException;
import com.yomahub.liteflow.exception.NullNodeTypeException;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.parser.el.LocalJsonFlowELParser;
import com.yomahub.liteflow.parser.el.LocalXmlFlowELParser;
import com.yomahub.liteflow.parser.el.LocalYmlFlowELParser;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.ScriptExecutorFactory;
import com.yomahub.liteflow.script.exception.ScriptLoadException;
import com.yomahub.liteflow.script.exception.ScriptSpiException;
import com.yomahub.liteflow.spi.ContextAware;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.spi.local.LocalContextAware;
import com.yomahub.liteflow.util.CopyOnWriteHashMap;
import com.yomahub.liteflow.util.LiteFlowProxyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 流程元数据类
 * @author Bryan.Zhang
 */
public class FlowBus {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBus.class);

    private static final Map<String, Chain> chainMap = new CopyOnWriteHashMap<>();

    private static final Map<String, Node> nodeMap = new CopyOnWriteHashMap<>();

    private FlowBus() {
    }

    public static Chain getChain(String id){
        return chainMap.get(id);
    }

    //这一方法主要用于第一阶段chain的预装载
    public static void addChain(String chainName){
        if (!chainMap.containsKey(chainName)){
            chainMap.put(chainName, new Chain(chainName));
        }
    }

    //这个方法主要用于第二阶段的替换chain
    public static void addChain(Chain chain) {
        chainMap.put(chain.getChainId(), chain);
    }

    public static boolean containChain(String chainId) {
        return chainMap.containsKey(chainId);
    }

    public static boolean needInit() {
        return MapUtil.isEmpty(chainMap);
    }

    public static boolean containNode(String nodeId) {
        return nodeMap.containsKey(nodeId);
    }

    public static void addSpringScanNode(String nodeId, NodeComponent nodeComponent) {
        //根据class来猜测类型
        NodeTypeEnum type = NodeTypeEnum.guessType(nodeComponent.getClass());

        if (type == null){
            throw new NullNodeTypeException(StrUtil.format("node type is null for node[{}]", nodeId));
        }

        nodeMap.put(nodeId, new Node(ComponentInitializer.loadInstance().initComponent(nodeComponent, type, null, nodeId)));
    }

    public static void addCommonNode(String nodeId, String name, String cmpClazzStr){
        Class<?> cmpClazz;
        try{
            cmpClazz = Class.forName(cmpClazzStr);
        }catch (Exception e){
            throw new ComponentCannotRegisterException(e.getMessage());
        }
        addNode(nodeId, name, NodeTypeEnum.COMMON, cmpClazz, null);
    }

    public static void addCommonNode(String nodeId, String name, Class<?> cmpClazz){
        addNode(nodeId, name, NodeTypeEnum.COMMON, cmpClazz, null);
    }

    public static void addSwitchNode(String nodeId, String name, String cmpClazzStr){
        Class<?> cmpClazz;
        try{
            cmpClazz = Class.forName(cmpClazzStr);
        }catch (Exception e){
            throw new ComponentCannotRegisterException(e.getMessage());
        }
        addNode(nodeId, name, NodeTypeEnum.SWITCH, cmpClazz, null);
    }

    public static void addSwitchNode(String nodeId, String name, Class<?> cmpClazz){
        addNode(nodeId, name, NodeTypeEnum.SWITCH, cmpClazz, null);
    }

    public static void addIfNode(String nodeId, String name, String cmpClazzStr){
        Class<?> cmpClazz;
        try{
            cmpClazz = Class.forName(cmpClazzStr);
        }catch (Exception e){
            throw new ComponentCannotRegisterException(e.getMessage());
        }
        addNode(nodeId, name, NodeTypeEnum.IF, cmpClazz, null);
    }

    public static void addIfNode(String nodeId, String name, Class<?> cmpClazz){
        addNode(nodeId, name, NodeTypeEnum.IF, cmpClazz, null);
    }

    public static void addForNode(String nodeId, String name, String cmpClazzStr){
        Class<?> cmpClazz;
        try{
            cmpClazz = Class.forName(cmpClazzStr);
        }catch (Exception e){
            throw new ComponentCannotRegisterException(e.getMessage());
        }
        addNode(nodeId, name, NodeTypeEnum.FOR, cmpClazz, null);
    }

    public static void addForNode(String nodeId, String name, Class<?> cmpClazz){
        addNode(nodeId, name, NodeTypeEnum.FOR, cmpClazz, null);
    }

    public static void addWhileNode(String nodeId, String name, String cmpClazzStr){
        Class<?> cmpClazz;
        try{
            cmpClazz = Class.forName(cmpClazzStr);
        }catch (Exception e){
            throw new ComponentCannotRegisterException(e.getMessage());
        }
        addNode(nodeId, name, NodeTypeEnum.WHILE, cmpClazz, null);
    }

    public static void addWhileNode(String nodeId, String name, Class<?> cmpClazz){
        addNode(nodeId, name, NodeTypeEnum.WHILE, cmpClazz, null);
    }

    public static void addBreakNode(String nodeId, String name, String cmpClazzStr){
        Class<?> cmpClazz;
        try{
            cmpClazz = Class.forName(cmpClazzStr);
        }catch (Exception e){
            throw new ComponentCannotRegisterException(e.getMessage());
        }
        addNode(nodeId, name, NodeTypeEnum.BREAK, cmpClazz, null);
    }

    public static void addBreakNode(String nodeId, String name, Class<?> cmpClazz){
        addNode(nodeId, name, NodeTypeEnum.BREAK, cmpClazz, null);
    }

    public static void addCommonScriptNode(String nodeId, String name, String script){
        addNode(nodeId, name, NodeTypeEnum.SCRIPT, ScriptCommonComponent.class, script);
    }

    public static void addSwitchScriptNode(String nodeId, String name, String script){
        addNode(nodeId, name, NodeTypeEnum.SWITCH_SCRIPT, ScriptSwitchComponent.class, script);
    }

    public static void addIfScriptNode(String nodeId, String name, String script){
        addNode(nodeId, name, NodeTypeEnum.IF_SCRIPT, ScriptIfComponent.class, script);
    }

    public static void addForScriptNode(String nodeId, String name, String script){
        addNode(nodeId, name, NodeTypeEnum.FOR_SCRIPT, ScriptForComponent.class, script);
    }

    public static void addWhileScriptNode(String nodeId, String name, String script){
        addNode(nodeId, name, NodeTypeEnum.WHILE_SCRIPT, ScriptWhileComponent.class, script);
    }

    public static void addBreakScriptNode(String nodeId, String name, String script){
        addNode(nodeId, name, NodeTypeEnum.BREAK_SCRIPT, ScriptBreakComponent.class, script);
    }

    private static void addNode(String nodeId, String name, NodeTypeEnum type, Class<?> cmpClazz, String script) {
        try {
            //判断此类是否是声明式的组件，如果是声明式的组件，就用动态代理生成实例
            //如果不是声明式的，就用传统的方式进行判断
            List<NodeComponent> cmpInstances = new ArrayList<>();
            if (LiteFlowProxyUtil.isDeclareCmp(cmpClazz)){
                //这里的逻辑要仔细看下
                //如果是spring体系，把原始的类往spring上下文中进行注册，那么会走到ComponentScanner中
                //由于ComponentScanner中已经对原始类进行了动态代理，出来的对象已经变成了动态代理类，所以这时候的bean已经是NodeComponent的子类了
                //所以spring体系下，无需再对这个bean做二次代理
                //但是在非spring体系下，这个bean依旧是原来那个bean，所以需要对这个bean做一次代理
                //这里用ContextAware的spi机制来判断是否spring体系
                ContextAware contextAware = ContextAwareHolder.loadContextAware();
                Object bean = ContextAwareHolder.loadContextAware().registerBean(nodeId, cmpClazz);
                if (LocalContextAware.class.isAssignableFrom(contextAware.getClass())){
                    cmpInstances = LiteFlowProxyUtil.proxy2NodeComponent(bean, nodeId);
                }else {
                    cmpInstances = ListUtil.toList((NodeComponent) bean);
                }
            }else{
                //以node方式配置，本质上是为了适配无spring的环境，如果有spring环境，其实不用这么配置
                //这里的逻辑是判断是否能从spring上下文中取到，如果没有spring，则就是new instance了
                //如果是script类型的节点，因为class只有一个，所以也不能注册进spring上下文，注册的时候需要new Instance
                if (!type.isScript()){
                    cmpInstances = ListUtil.toList((NodeComponent) ContextAwareHolder.loadContextAware().registerOrGet(nodeId, cmpClazz));
                }
                // 去除null元素
                cmpInstances.remove(null);
                // 如果为空
                if (cmpInstances.isEmpty()) {
                    NodeComponent cmpInstance = (NodeComponent) cmpClazz.newInstance();
                    cmpInstances.add(cmpInstance);
                }
            }
            //进行初始化
            cmpInstances = cmpInstances.stream()
                    .map(cmpInstance -> ComponentInitializer.loadInstance().initComponent(
                            cmpInstance,
                            type,
                            name,
                            cmpInstance.getNodeId() == null ? nodeId : cmpInstance.getNodeId())
                    ).collect(Collectors.toList());

            //初始化Node
            List<Node> nodes = cmpInstances.stream().map(Node::new).collect(Collectors.toList());


            for (int i = 0; i < nodes.size(); i++) {
                Node node = nodes.get(i);
                NodeComponent cmpInstance = cmpInstances.get(i);
                //如果是脚本节点，则还要加载script脚本
                if (type.isScript()){
                    if (StrUtil.isNotBlank(script)){
                        node.setScript(script);
                        ((ScriptComponent)cmpInstance).loadScript(script);
                    }else{
                        String errorMsg = StrUtil.format("script for node[{}] is empty", nodeId);
                        throw new ScriptLoadException(errorMsg);
                    }
                }

                String activeNodeId = StrUtil.isEmpty(cmpInstance.getNodeId()) ? nodeId : cmpInstance.getNodeId();
                nodeMap.put(activeNodeId, node);
            }

        } catch (Exception e) {
            String error = StrUtil.format("component[{}] register error", StrUtil.isEmpty(name)?nodeId:StrUtil.format("{}({})",nodeId,name));
            LOG.error(e.getMessage());
            throw new ComponentCannotRegisterException(error);
        }
    }

    public static Node getNode(String nodeId) {
        return nodeMap.get(nodeId);
    }

    //虽然实现了cloneable，但是还是浅copy，因为condNodeMap这个对象还是共用的。
    //那condNodeMap共用有关系么，原则上没有关系。但是从设计理念上，以后应该要分开
    //tag和condNodeMap这2个属性不属于全局概念，属于每个chain范围的属性
    public static Node copyNode(String nodeId) {
        try{
            Node node = nodeMap.get(nodeId);
            return node.copy();
        }catch (Exception e){
            return null;
        }
    }

    public static Map<String, Node> getNodeMap(){
        return nodeMap;
    }

    public static Map<String, Chain> getChainMap(){
        return chainMap;
    }

    public static void cleanCache() {
        chainMap.clear();
        nodeMap.clear();
        cleanScriptCache();
    }

    public static void cleanScriptCache() {
        //如果引入了脚本组件SPI，则还需要清理脚本的缓存
        try{
            ScriptExecutor scriptExecutor = ScriptExecutorFactory.loadInstance().getScriptExecutor();
            if (ObjectUtil.isNotNull(scriptExecutor)){
                scriptExecutor.cleanCache();
            }
        }catch (ScriptSpiException ignored){}
    }

    public static void refreshFlowMetaData(FlowParserTypeEnum type, String content) throws Exception {
        if (type.equals(FlowParserTypeEnum.TYPE_EL_XML)) {
            new LocalXmlFlowELParser().parse(content);
        } else if (type.equals(FlowParserTypeEnum.TYPE_EL_JSON)) {
            new LocalJsonFlowELParser().parse(content);
        } else if (type.equals(FlowParserTypeEnum.TYPE_EL_YML)) {
            new LocalYmlFlowELParser().parse(content);
        }
    }

    public static boolean removeChain(String chainId){
        if (containChain(chainId)){
            chainMap.remove(chainId);
            return true;
        }else{
            String errMsg = StrUtil.format("cannot find the chain[{}]", chainId);
            LOG.error(errMsg);
            return false;
        }
    }

    public static void removeChain(String... chainIds){
        Arrays.stream(chainIds).forEach(FlowBus::removeChain);
    }
}
