/*
 * Copyright (C) 2016 alchemystar, Inc. All Rights Reserved.
 */
package live.sidian.database.synchronizer.model;


import live.sidian.database.synchronizer.utils.SqlUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数据库元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetaData {

    /**
     * 数据库连接
     */
    private Connection connection;
    /**
     * 用户
     */
    private String user;
    /**
     * 密码
     */
    private String password;
    /**
     * 库名
     */
    private String schema;
    /**
     * 连接url
     */
    private String jdbcUrl;

    /**
     * 所有的表
     */
    @Builder.Default
    private Map<String, Table> tables = new LinkedHashMap<String, Table>();

}
