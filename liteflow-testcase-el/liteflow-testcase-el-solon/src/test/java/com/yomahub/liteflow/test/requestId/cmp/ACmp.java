/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.test.requestId.cmp;

import com.yomahub.liteflow.annotation.LiteflowMethod;
import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.enums.LiteFlowMethodEnum;
import org.noear.solon.annotation.Component;

@Component("a")
public class ACmp {

    @LiteflowMethod(LiteFlowMethodEnum.PROCESS)
    public void process(NodeComponent bindCmp) {
        System.out.println("ACmp executed!");
    }
}
