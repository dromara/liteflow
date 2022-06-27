/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.privateDelivery.cmp;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;

@LiteflowComponent("b")
public class BCmp extends NodeComponent {

    @Override
    public void process() {
        System.out.println("BCmp executed!");
        Integer value = this.getPrivateDeliveryData();
        DefaultContext context = this.getFirstContextBean();
        ConcurrentHashSet<Integer> testSet = context.getData("testSet");
        testSet.add(value);
    }
}

