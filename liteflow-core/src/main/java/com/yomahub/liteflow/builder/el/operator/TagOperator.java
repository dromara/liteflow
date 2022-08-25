package com.yomahub.liteflow.builder.el.operator;

import cn.hutool.core.util.ArrayUtil;
import com.ql.util.express.Operator;
import com.ql.util.express.exception.QLException;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.element.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EL规则中的tag的操作符
 * @author Bryan.Zhang
 * @since 2.8.0
 */
public class TagOperator extends Operator {

    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Override
    public Node executeInner(Object[] objects) throws Exception {
        try {
            if (ArrayUtil.isEmpty(objects)) {
                throw new QLException("parameter is empty");
            }

            if (objects.length != 2) {
                throw new QLException("parameter error");
            }

            Node node;
            if (objects[0] instanceof Node) {
                node = (Node) objects[0];
            } else {
                throw new QLException("The caller must be Node item");
            }

            String tag = null;
            if (objects[1] instanceof String) {
                tag = objects[1].toString();
            } else {
                throw new QLException("the parameter must be String type");
            }

            //这里为什么要clone一个呢？
            //因为tag是跟着chain走的。而在el上下文里的放的都是同一个node，如果多个同样的node tag不同，则这里必须copy
            Node copyNode = FlowBus.copyNode(node.getId());

            copyNode.setTag(tag);

            return copyNode;

        }catch (QLException e){
            throw e;
        }catch (Exception e){
            throw new ELParseException("errors occurred in EL parsing");
        }
    }
}
