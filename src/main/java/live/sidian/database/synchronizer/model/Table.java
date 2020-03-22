/*
 * Copyright (C) 2016 alchemystar, Inc. All Rights Reserved.
 */
package live.sidian.database.synchronizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Table {
    /**
     * 表名
     */
    private String name;
    /**
     * 建表语句
     */
    private String createTableSQL;
    /**
     * 表的全部字段
     */
    @Builder.Default
    private Map<String, Column> columns = new LinkedHashMap<>();
    /**
     * 全部索引
     */
    @Builder.Default
    private Map<String, Index> indexes = new LinkedHashMap<>();
}
