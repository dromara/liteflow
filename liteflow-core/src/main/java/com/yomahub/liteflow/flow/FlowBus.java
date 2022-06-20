/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.flow;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.ComponentInitializer;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.core.ScriptComponent;
import com.yomahub.liteflow.core.ScriptCondComponent;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.enums.FlowParserTypeEnum;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.ComponentCannotRegisterException;
import com.yomahub.liteflow.parser.LocalJsonFlowParser;
import com.yomahub.liteflow.parser.LocalXmlFlowParser;
import com.yomahub.liteflow.parser.LocalYmlFlowParser;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.ScriptExecutorFactory;
import com.yomahub.liteflow.script.exception.ScriptSpiException;
import com.yomahub.liteflow.spi.ContextAware;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.spi.local.LocalContextAware;
import com.yomahub.liteflow.util.CopyOnWriteHashMap;
import com.yomahub.liteflow.util.LiteFlowProxyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

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
        chainMap.put(chain.getChainName(), chain);
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
        nodeMap.put(nodeId, new Node(ComponentInitializer.loadInstance().initComponent(nodeComponent, NodeTypeEnum.COMMON, null, nodeId)));
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

    public static void addCommonNode(String nodeId, String name, Class<? extends NodeComponent> cmpClazz){
        addNode(nodeId, name, NodeTypeEnum.COMMON, cmpClazz, null);
    }

    public static void addCommonScriptNode(String nodeId, String name, String script){
        addNode(nodeId, name, NodeTypeEnum.SCRIPT, ScriptComponent.class, script);
    }

    public static void addSwitchScriptNode(String nodeId, String name, String script){
        addNode(nodeId, name, NodeTypeEnum.SWITCH_SCRIPT, ScriptCondComponent.class, script);
    }

    private static void addNode(String nodeId, String name, NodeTypeEnum type, Class<?> cmpClazz, String script) {
        try {
            //判断此类是否是声明式的组件，如果是声明式的组件，就用动态代理生成实例
            //如果不是声明式的，就用传统的方式进行判断
            NodeComponent cmpInstance = null;
            if (LiteFlowProxyUtil.isMarkedCmp(cmpClazz)){
                //这里的逻辑要仔细看下
                //如果是spring体系，把原始的类往spring上下文中进行注册，那么会走到ComponentScanner中
                //由于ComponentScanner中已经对原始类进行了动态代理，出来的对象已经变成了动态代理类，所以这时候的bean已经是NodeComponent的子类了
                //所以spring体系下，无需再对这个bean做二次代理
                //但是在非spring体系下，这个bean依旧是原来那个bean，所以需要对这个bean做一次代理
                //这里用ContextAware的spi机制来判断是否spring体系
                ContextAware contextAware = ContextAwareHolder.loadContextAware();
                Object bean = ContextAwareHolder.loadContextAware().registerBean(nodeId, cmpClazz);
                if (LocalContextAware.class.isAssignableFrom(contextAware.getClass())){
                    cmpInstance = LiteFlowProxyUtil.proxy2NodeComponent(bean, nodeId);
                }else {
                    cmpInstance = (NodeComponent) bean;
                }
            }else{
                //以node方式配置，本质上是为了适配无spring的环境，如果有spring环境，其实不用这么配置
                //这里的逻辑是判断是否能从spring上下文中取到，如果没有spring，则就是new instance了
                //如果是script类型的节点，因为class只有一个，所以也不能注册进spring上下文，注册的时候需要new Instance
                if (!CollectionUtil.newArrayList(NodeTypeEnum.SCRIPT, NodeTypeEnum.SWITCH_SCRIPT).contains(type)){
                    cmpInstance = (NodeComponent) ContextAwareHolder.loadContextAware().registerOrGet(nodeId, cmpClazz);
                }

                if (ObjectUtil.isNull(cmpInstance)) {
                    cmpInstance = (NodeComponent) cmpClazz.newInstance();
                }
            }

            //进行初始化
            cmpInstance = ComponentInitializer.loadInstance().initComponent(cmpInstance, type, name, nodeId);

            //初始化Node
            Node node = new Node(cmpInstance);

            //如果是脚本节点(普通脚本节点/条件脚本节点)，则还要加载script脚本
            if (StrUtil.isNotBlank(script)){
                node.setScript(script);
                if (type.equals(NodeTypeEnum.SCRIPT)){
                    ((ScriptComponent)cmpInstance).loadScript(script);
                }else if(type.equals(NodeTypeEnum.SWITCH_SCRIPT)){
                    ((ScriptCondComponent)cmpInstance).loadScript(script);
                }
            }

            nodeMap.put(nodeId, node);
        } catch (Exception e) {
            String error = StrUtil.format("component[{}] register error", StrUtil.isEmpty(name)?nodeId:StrUtil.format("{}({})",nodeId,name));
            LOG.error(error, e);
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
        if (type.equals(FlowParserTypeEnum.TYPE_XML)) {
            new LocalXmlFlowParser().parse(content);
        } else if (type.equals(FlowParserTypeEnum.TYPE_JSON)) {
            new LocalJsonFlowParser().parse(content);
        } else if (type.equals(FlowParserTypeEnum.TYPE_YML)) {
            new LocalYmlFlowParser().parse(content);
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
}
