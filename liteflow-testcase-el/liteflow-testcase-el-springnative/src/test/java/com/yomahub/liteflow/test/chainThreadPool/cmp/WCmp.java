/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.chainThreadPool.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import org.springframework.stereotype.Component;

@Component("w")
public class WCmp extends NodeComponent {

    @Override
    public void process() {
        DefaultContext context = this.getFirstContextBean();
        context.setData("threadName", Thread.currentThread().getName());
        System.out.println("WCmp executed!");
    }

}
