/*
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Xu Qiaolun
 * @email jamesxql@gmail.com
 * @Date 2024/9/5
 */
package com.yomahub.liteflow.test.cmpData.cmp;

import com.yomahub.liteflow.core.NodeComponent;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.cmpData.vo.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("d")
public class DCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        List<User> users = getCmpDataList(User.class);
        DefaultContext context = getFirstContextBean();
        context.setData("users", users);
        System.out.println("DCmp executed!");
    }
}
