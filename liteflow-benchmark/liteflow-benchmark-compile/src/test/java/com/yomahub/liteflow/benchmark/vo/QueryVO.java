package com.yomahub.liteflow.benchmark.vo;

public class QueryVO {

    //渠道名称
    private String channel;

    //剩余短信包数量
    private int availCount;

    public QueryVO(String channel, int availCount) {
        this.channel = channel;
        this.availCount = availCount;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public int getAvailCount() {
        return availCount;
    }

    public void setAvailCount(int availCount) {
        this.availCount = availCount;
    }
}
