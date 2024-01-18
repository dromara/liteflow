package com.yomahub.liteflow.test.retryTimes.cmp;


import com.yomahub.liteflow.core.NodeWhileComponent;

public class NCmp extends NodeWhileComponent {
    int flag = 0;

    @Override
    public boolean processWhile() throws Exception {
        flag ++;
        System.out.println("NCmp executed!");
        if(flag < 4) throw new RuntimeException();
        else return flag == 4 ? true : false;
    }
}
