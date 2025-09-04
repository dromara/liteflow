package com.yomahub.liteflow.benchmark.enums;

public enum  OrderChannelEnum {
    APP(1,"APP渠道"),
    MINI_PROGRAM(2,"小程序渠道"),
    WX_H5(3,"微信H5"),
    MOBILE_H5(4,"移动H5渠道"),
    WEB_PORTAL(5,"PC主站渠道"),
    OFFLINE_STORE(5,"线下门店渠道");


    private Integer id;

    private String name;

    OrderChannelEnum(int id, String name){
        this.id = id;
        this.name = name;
    }
}
