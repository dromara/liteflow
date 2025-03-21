package com.yomahub.liteflow.meta;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.flow.instanceId.NodeInstanceIdManageSpiHolder;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
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
     * 找出含有指定nodeId的chain对象
     * @param nodeId 节点Id
     * @return Chain对象列表
     */
    public static List<Chain> getChainsContainsNodeId(String nodeId){
        return FlowBus.getChainMap().values().stream().filter(
            chain -> getNodes(chain.getChainId()).stream().anyMatch(
                    node -> node.getId().equals(nodeId)
            )
        ).collect(Collectors.toList());
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
     * 从任意Executable对象中取到Node列表
     * @param executable 可执行对象，包括Chain，Condition，Node
     * @return 节点列表
     */
    public static List<Node> getNodes(Executable executable){
        if (executable instanceof Chain){
            Chain chain = (Chain) executable;
            return chain.getConditionList().stream().flatMap(
                    (Function<Condition, Stream<Node>>) condition -> getNodes(condition).stream()
            ).collect(Collectors.toList());
        }else if(executable instanceof Condition){
            Condition condition = (Condition) executable;
            return condition.getExecutableGroup().entrySet().stream().flatMap(
                    (Function<Map.Entry<String, List<Executable>>, Stream<Executable>>) entry -> entry.getValue().stream()
            ).flatMap(
                    (Function<Executable, Stream<Node>>) item -> getNodes(item).stream()
            ).collect(Collectors.toList());
        }else if(executable instanceof Node){
            return CollectionUtil.toList((Node) executable);
        }else{
            return ListUtil.empty();
        }
    }

    /**
     * 通过chainId获得这个chain中所有的Node
     * @param chainId chain的Id
     * @return 指定chain中的所有Node
     */
    public static List<Node> getNodes(String chainId){
        return getNodes(getChain(chainId));
    }

    /**
     * 通过chainId和nodeId获得Node列表
     * @param chainId chain的Id
     * @param nodeId 节点Id
     * @return Node对象列表，一个节点在Chain里有可能出现多次
     */
    public static List<Node> getNodes(String chainId, String nodeId){
        return getNodes(chainId).stream().filter(
                node -> nodeId.equals(node.getId())
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
     * 只有打开liteflow.enable-node-instance-id=true才会正常调用这个
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
     * 通过nodeId找到在所有Chain中存在的Node对象列表
     * @param nodeId Node实例id
     * @return Node对象列表
     */
    public static List<Node> getNodesInAllChain(String nodeId){
        return FlowBus.getChainMap().values().stream().flatMap(
            (Function<Chain, Stream<Node>>) chain -> Objects.requireNonNull(getNodes(chain.getChainId(), nodeId)).stream().filter(
                node -> node.getId().equals(nodeId)
            )
        ).collect(Collectors.toList());
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
