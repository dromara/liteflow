package com.yomahub.liteflow.parser.sql.read;

import java.util.List;


public interface CustomSqlRead<T> {
    List<T> getCustomChain();
}
