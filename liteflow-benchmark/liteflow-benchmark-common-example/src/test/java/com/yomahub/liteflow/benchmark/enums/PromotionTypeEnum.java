package com.yomahub.liteflow.benchmark.enums;

public enum PromotionTypeEnum {
    FULL_CUT(1, "满减"),
    FULL_DISCOUNT(2, "满折"),
    RUSH_BUY(3, "抢购");

    private Integer id;

    private String name;

    PromotionTypeEnum(int id, String name){
        this.id = id;
        this.name = name;
    }
}
