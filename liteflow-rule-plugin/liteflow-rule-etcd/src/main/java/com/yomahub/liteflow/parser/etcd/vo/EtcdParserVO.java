package com.yomahub.liteflow.parser.etcd.vo;

/**
 * 用于解析RuleSourceExtData的vo类，用于etcd模式中
 * @author zendwang
 * @since 2.9.0
 */
public class EtcdParserVO {

    private String connectStr;

    private String nodePath;

    public String getConnectStr() {
        return connectStr;
    }

    public void setConnectStr(String connectStr) {
        this.connectStr = connectStr;
    }

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }
}
