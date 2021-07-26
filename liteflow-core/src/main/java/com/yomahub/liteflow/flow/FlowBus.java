/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.flow;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.entity.flow.Chain;
import com.yomahub.liteflow.entity.flow.Node;
import com.yomahub.liteflow.enums.FlowParserTypeEnum;
import com.yomahub.liteflow.exception.ComponentCannotRegisterException;
import com.yomahub.liteflow.parser.LocalJsonFlowParser;
import com.yomahub.liteflow.parser.LocalXmlFlowParser;
import com.yomahub.liteflow.parser.LocalYmlFlowParser;
import com.yomahub.liteflow.util.SpringAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 流程元数据类
 * @author Bryan.Zhang
 */
public class FlowBus {

    private static final Logger LOG = LoggerFactory.getLogger(FlowBus.class);

    private static final Map<String, Chain> chainMap = new HashMap<>();

    private static final Map<String, Node> nodeMap = new HashMap<>();

    private FlowBus() {
    }

    public static Chain getChain(String id) throws Exception {
        if (MapUtil.isEmpty(chainMap)) {
            throw new Exception("please config the rule first");
        }
        return chainMap.get(id);
    }

    public static void addChain(String name, Chain chain) {
        chainMap.put(name, chain);
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

    public static void addNode(String nodeId, Node node) {
        if (containNode(nodeId)) return;
        nodeMap.put(nodeId, node);
    }

    public static void addNode(String nodeId, String name, String cmpClazzStr) throws Exception {
        if (containNode(nodeId)) return;
        Class<NodeComponent> cmpClazz = (Class<NodeComponent>) Class.forName(cmpClazzStr);
        addNode(nodeId, name ,cmpClazz);
    }

    public static void addNode(String nodeId, Class<? extends NodeComponent> cmpClazz){
        addNode(nodeId, null, cmpClazz);
    }

    public static void addNode(String nodeId, String name, Class<? extends NodeComponent> cmpClazz) {
        if (containNode(nodeId)) return;
        try {
            //以node方式配置，本质上是为了适配无spring的环境，如果有spring环境，其实不用这么配置
            //这里的逻辑是判断是否能从spring上下文中取到，如果没有spring，则就是new instance了
            NodeComponent cmpInstance = SpringAware.registerOrGet(cmpClazz);
            if (ObjectUtil.isNull(cmpInstance)) {
                LOG.warn("couldn't find component class [{}] from spring context", cmpClazz.getName());
                cmpInstance = cmpClazz.newInstance();
            }
            cmpInstance.setNodeId(nodeId);
            cmpInstance.setName(name);
            cmpInstance.setSelf(cmpInstance);
            nodeMap.put(nodeId, new Node(cmpInstance));
        } catch (Exception e) {
            String error = StrUtil.format("component[{}] register error", cmpClazz.getName());
            LOG.error(error, e);
            throw new ComponentCannotRegisterException(error);
        }
    }

    public static Node getNode(String nodeId) {
        return nodeMap.get(nodeId);
    }

    public static void cleanCache() {
        chainMap.clear();
        nodeMap.clear();
    }

    //目前这种方式刷新不完全平滑
    public static void refreshFlowMetaData(FlowParserTypeEnum type, String content) throws Exception {
        FlowBus.cleanCache();
        if (type.equals(FlowParserTypeEnum.TYPE_XML)) {
            new LocalXmlFlowParser().parse(content);
        } else if (type.equals(FlowParserTypeEnum.TYPE_JSON)) {
            new LocalJsonFlowParser().parse(content);
        } else if (type.equals(FlowParserTypeEnum.TYPE_YML)) {
            new LocalYmlFlowParser().parse(content);
        }
    }
}
