
package com.yomahub.liteflow.parser.etcd.exception;

/**
 * Etcd解析异常
 *
 * @author zendwang
 * @since 2.9.0
 */
public class EtcdException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 异常信息
     */
    private String message;

    public EtcdException(String message) {
        this.message = message;
    }

    public EtcdException(Throwable cause) {
        super(cause);
        this.message = cause.getMessage();
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
