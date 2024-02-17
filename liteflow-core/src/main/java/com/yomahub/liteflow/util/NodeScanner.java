package com.yomahub.liteflow.util;

import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.flow.element.Executable;
import com.yomahub.liteflow.flow.element.Node;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * 节点扫描器
 *
 * @author DaleLee
 * @since 2.12.0
 */
public class NodeScanner {

    /**
     * 获取 Chain 中的所有 Node
     *
     * @param chain Chain
     * @return Node 集合
     */
    public static List<Node> getNodesInChain(Chain chain) {
        List<Node> result = new ArrayList<>();
        if (chain == null) {
            return result;
        }
        for (Condition condition : chain.getConditionList()) {
            result.addAll(getNodesInCondition(condition));
        }
        return result;
    }

    /**
     * 获取 Condition 中的所有 Node
     *
     * @param condition Condition
     * @return Node 集合
     */
    public static List<Node> getNodesInCondition(Condition condition) {
        List<Node> result = new ArrayList<>();
        if (condition == null) {
            return result;
        }

        // 层序遍历
        Queue<Executable> queue = new LinkedList<>();
        queue.offer(condition);

        while (!queue.isEmpty()) {
            Executable cur = queue.poll();
            if (cur instanceof Condition) {
                Map<String, List<Executable>> executableGroup = ((Condition) cur).getExecutableGroup();
                for (List<Executable> executables : executableGroup.values()) {
                    executables.forEach(queue::offer);
                }
            } else if (cur instanceof Node) {
                result.add((Node) cur);
            }
        }

        return result;
    }
}
