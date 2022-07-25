package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ql.util.express.Operator;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Node;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

/**
 * EL规则中的node的操作符
 * @author Bryan.Zhang
 * @since 2.8.3
 */
public class NodeOperator extends Operator {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Override
    public Object executeInner(Object[] objects) throws Exception {
        if (ArrayUtil.isEmpty(objects)){
            throw new Exception();
        }

        if (objects.length != 1){
            LOG.error("parameter error");
            throw new Exception();
        }

        String nodeId;
        if (objects[0] instanceof String){
            nodeId = (String) objects[0];
        }else{
            LOG.error("The value must be Node item!");
            throw new Exception();
        }

        if (FlowBus.containNode(nodeId)){
            return FlowBus.getNode(nodeId);
        }else{
            LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();
            if (StrUtil.isNotBlank(liteflowConfig.getSubstituteCmpClass())){
                Node substituteNode = FlowBus.getNodeMap().values().stream().filter(node
                        -> node.getInstance().getClass().getName().equals(liteflowConfig.getSubstituteCmpClass())).findFirst().orElse(null);
                if (ObjectUtil.isNotNull(substituteNode)){
                    return substituteNode;
                }else{
                    String error = StrUtil.format("This node[{}] cannot be found", nodeId);
                    LOG.error(error);
                    throw new Exception();
                }
            }else{
                String error = StrUtil.format("This node[{}] cannot be found, or you can configure an substitute node", nodeId);
                LOG.error(error);
                throw new Exception();
            }
        }
    }
}
