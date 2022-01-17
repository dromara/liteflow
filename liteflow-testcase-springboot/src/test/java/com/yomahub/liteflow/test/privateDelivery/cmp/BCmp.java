/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.privateDelivery.cmp;

import cn.hutool.core.collection.CollUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.Set;

@LiteflowComponent("b")
public class BCmp extends NodeComponent {

    @Override
    public boolean isAccess() {
        Queue<Integer> values = this.getSlot().getRequestData();
        System.out.println("BCmp executed! values.size" + values.size());
        if (CollUtil.isEmpty(values)) {
            return false;
        }
        Integer value = values.poll();
        if (value == null) {
            return false;
        }
        this.sendPrivateDeliveryData(this.getNodeId(), value);
        return true;
    }

    @Override
    public void process() {
        Integer value = getPrivateDeliveryData();
        System.out.println("BCmp executed!" + value);
    }
}
