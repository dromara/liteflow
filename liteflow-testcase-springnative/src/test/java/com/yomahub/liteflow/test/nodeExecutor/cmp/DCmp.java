/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.nodeExecutor.cmp;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.annotation.LiteflowRetry;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.flow.executor.NodeExecutor;
import com.yomahub.liteflow.test.nodeExecutor.CustomerNodeExecutorAndCustomRetry;

@LiteflowComponent("d")
@LiteflowRetry(retry = 5, forExceptions = {NullPointerException.class})
public class DCmp extends NodeComponent {

    @Override
    public void process() {
        System.out.println("DCmp executed!");
        throw new NullPointerException("demo exception");
    }

    @Override
    public Class<? extends NodeExecutor> getNodeExecutorClass() {
        return CustomerNodeExecutorAndCustomRetry.class;
    }
}
