package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ql.util.express.Operator;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.exception.ELParseException;
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
    public Node executeInner(Object[] objects) throws Exception {
        try{
            if (ArrayUtil.isEmpty(objects)){
                throw new QLException("parameter is empty");
            }

            if (objects.length != 1){
                throw new QLException("parameter error");
            }

            String nodeId;
            if (objects[0] instanceof String){
                nodeId = (String) objects[0];
            }else{
                throw new QLException("The value must be Node item!");
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
                        throw new QLException(error);
                    }
                }else{
                    String error = StrUtil.format("This node[{}] cannot be found, or you can configure an substitute node", nodeId);
                    throw new QLException(error);
                }
            }
        }catch (QLException e){
            throw e;
        }catch (Exception e){
            throw new ELParseException("errors occurred in EL parsing");
        }
    }
}
