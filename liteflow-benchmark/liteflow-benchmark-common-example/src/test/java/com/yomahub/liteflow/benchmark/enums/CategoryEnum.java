package com.yomahub.liteflow.benchmark.enums;

public enum  CategoryEnum {
    FOOD(1,"食品"),
    CLOTHES(2,"衣服"),
    DAILY_USE(3,"生活用品");

    private Integer id;

    private String name;

    CategoryEnum(int id, String name){
        this.id = id;
        this.name = name;
    }
}
