package com.yomahub.liteflow.meta;

import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.instanceId.NodeInstanceIdManageSpiHolder;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * LiteFlow元数据统一操作类
 * @since 2.13.0
 * @author Bryan.Zhang
 */
public class LiteflowMetaOperator {

    //--------------------------------------------Chain相关---------------------------------------------

    /**
     * 通过chainId获得Chain对象
     * @param chainId Chain的Id
     * @return Chain Chain对象
     */
    public static Chain getChain(String chainId){
        return FlowBus.getChain(chainId);
    }

    /**
     * 刷新所有的规则
     * 可以手动重新从ruleSource指定的数据源进行刷新
     * 此刷新操作将会刷新所有的规则
     */
    public static void reloadAllChain(){
        FlowExecutorHolder.loadInstance().reloadRule();
    }

    /**
     * 刷新某一个规则
     * @param chainId chain的Id
     * @param el 规则EL表达式
     */
    public static void reloadOneChain(String chainId, String el){
        FlowBus.reloadChain(chainId, el);
    }

    /**
     * 刷新某一个规则(带决策路由)
     * @param chainId chain的Id
     * @param el 规则EL表达式
     * @param routeEl 决策路由EL表达式
     */
    public static void reloadOneChain(String chainId, String el, String routeEl){
        FlowBus.reloadChain(chainId, el, routeEl);
    }

    /**
     * 从元数据中卸载掉一个Chain
     * @param chainId 需要卸载的chainId
     */
    public static void removeChain(String chainId){
        FlowBus.removeChain(chainId);
    }

    /**
     * 从元数据中卸载掉多个Chain
     * @param chainIds 多个chainId
     */
    public static void removeChain(String... chainIds){
        FlowBus.removeChain(chainIds);
    }

    //--------------------------------------------Node相关---------------------------------------------

    /**
     * 通过chainId获得这个chain中所有的Node
     * @param chainId chain的Id
     * @return 指定chain中的所有Node
     */
    public static List<Node> getNodes(String chainId){
        return FlowBus.getNodesByChainId(chainId);
    }

    /**
     * 通过chainId和nodeId获得Node列表
     * @param chainId chain的Id
     * @param nodeId 节点Id
     * @return Node对象列表，一个节点在Chain里有可能出现多次
     */
    public static List<Node> getNodes(String chainId, String nodeId){
        Chain chain = getChain(chainId);
        if (chain == null){
            return null;
        }
        return chain.getConditionList().stream().flatMap(
                (Function<Condition, Stream<Node>>) condition -> condition.getAllNodeInCondition().stream()
        ).filter(
                node -> node.getId().equals(nodeId)
        ).collect(Collectors.toList());
    }

    /**
     * 通过chainId和nodeInstanceId去获得具体的Node节点
     * 注意nodeInstance只有打开liteflow.enable-node-instance-id=true才会在Node对象中有
     * @param chainId chain的Id
     * @param nodeInstanceId Node节点的唯一Id
     * @return Node节点对象
     */
    public static Node getNode(String chainId, String nodeInstanceId){
        return NodeInstanceIdManageSpiHolder.getInstance().getNodeInstanceIdManageSpi().getNodeByIdAndInstanceId(chainId, nodeInstanceId);
    }

    /**
     * 通过chainId，nodeId，index去获取具体的Node节点
     * @param chainId chain的Id
     * @param nodeId 节点的Id
     * @param index 节点的序号下标，从0开始
     * @return Node节点对象
     */
    public static Node getNode(String chainId, String nodeId, int index){
        return NodeInstanceIdManageSpiHolder.getInstance().getNodeInstanceIdManageSpi().getNodeByIdAndIndex(chainId, nodeId, index);
    }

    /**
     * 通过chainId，nodeInstanceId去获取这个nodeInstanceId在Chain中的位置
     * 注意nodeInstance只有打开liteflow.enable-node-instance-id=true才会在Node对象中有
     * @param chainId chain的Id
     * @param nodeInstanceId Node的实例id
     * @return nodeInstanceId在这个chain中的位置，从0开始
     */
    public static int getNodeIndex(String chainId, String nodeInstanceId){
        return NodeInstanceIdManageSpiHolder.getInstance().getNodeInstanceIdManageSpi().getNodeLocationById(chainId, nodeInstanceId);
    }

    /**
     * 通过chainId，nodeId去获取这个节点的所有的instanceId
     * 注意nodeInstance只有打开liteflow.enable-node-instance-id=true才会在Node对象中有
     * @param chainId chain的Id
     * @param nodeId Node的实例id
     * @return 节点的instanceId列表
     */
    public static List<String> getNodeInstanceIds(String chainId, String nodeId){
        return NodeInstanceIdManageSpiHolder.getInstance().getNodeInstanceIdManageSpi().getNodeInstanceIds(chainId, nodeId);
    }

    /**
     * 刷新某一个脚本
     * @param nodeId 节点Id
     * @param script 刷新的脚本内容
     */
    public static void reloadScript(String nodeId, String script){
        FlowBus.reloadScript(nodeId, script);
    }
}
