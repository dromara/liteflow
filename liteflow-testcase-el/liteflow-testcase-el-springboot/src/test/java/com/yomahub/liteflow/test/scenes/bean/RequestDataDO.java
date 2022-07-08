package com.yomahub.liteflow.test.scenes.bean;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * springboot环境EL复杂例子测试1
 * 可以参与的营销 : 1条件:开始时间+区域.
 * @author nmnl
 */
public class RequestDataDO {
    // LocalDateTime.of(LocalDate.now(), LocalTime.of(15,0,0));
    private LocalDateTime localDateTime;

    private String instId;

    private RequestDataDO(LocalDateTime localDateTime, String instId) {
        this.localDateTime = localDateTime;
        this.instId = instId;
    }

    private static RequestDataDO create(LocalDateTime localDateTime, String instId) {
        return new RequestDataDO(localDateTime, instId);
    }

    public static RequestDataDO of(LocalDateTime localDateTime, String instId) {
        Objects.requireNonNull(localDateTime, "localDateTime");
        Objects.requireNonNull(instId, "instId");
        return create(localDateTime, instId);
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public String getInstId() {
        return instId;
    }

    public void setInstId(String instId) {
        this.instId = instId;
    }
}
