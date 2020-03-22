/*
 * Copyright (C) 2016 alchemystar, Inc. All Rights Reserved.
 */
package live.sidian.database.synchronizer.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Column {
    /**
     * 字段名
     */
    private String name;
    /**
     * 字段类型
     */
    private String type;
    /**
     * 是否可为null
     */
    private boolean nullable;
    /**
     * 默认值
     */
    private String defaultValue;
    /**
     * 注释
     */
    private String comment;
    /**
     * 其他约束
     */
    private String extra;


}
