package com.yomahub.liteflow.test.abstractChain.cmp;

import com.yomahub.liteflow.core.NodeIfComponent;
import org.noear.solon.annotation.Component;


@Component("c")
public class CCmp extends NodeIfComponent {
    @Override
    public boolean processIf() throws Exception {
        //do your biz
        return true;
    }
}

