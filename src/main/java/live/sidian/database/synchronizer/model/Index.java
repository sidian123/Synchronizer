/*
 * Copyright (C) 2016 alchemystar, Inc. All Rights Reserved.
 */
package live.sidian.database.synchronizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * 索引信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Index {

    /**
     * 与索引关联的列名
     */
    @Builder.Default
    private List<String> columns = new ArrayList<>();

    /**
     * 索引名
     */
    private String name;
    /**
     * 索引类型
     */
    private IndexType type;

    public enum IndexType{
        UNIQUE, INDEX
    }
}
