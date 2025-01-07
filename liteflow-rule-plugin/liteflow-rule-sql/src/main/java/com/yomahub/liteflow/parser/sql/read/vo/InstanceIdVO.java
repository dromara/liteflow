package com.yomahub.liteflow.parser.sql.read.vo;

/**
 * @author Jay li
 * @since 2.12.4
 */

public class InstanceIdVO {

    private String chainId;

    private String elDataMd5;

    private String nodeInstanceIdMapJson;

    public String getChainId() {
        return chainId;
    }

    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public String getElDataMd5() {
        return elDataMd5;
    }

    public void setElDataMd5(String elDataMd5) {
        this.elDataMd5 = elDataMd5;
    }

    public String getNodeInstanceIdMapJson() {
        return nodeInstanceIdMapJson;
    }

    public void setNodeInstanceIdMapJson(String nodeInstanceIdMapJson) {
        this.nodeInstanceIdMapJson = nodeInstanceIdMapJson;
    }
}
