/*
 * Copyright (C) 2016 alchemystar, Inc. All Rights Reserved.
 */
package live.sidian.database.synchronizer.utils;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlUtils {

    /**
     * 执行查询语句
     * @param conn
     * @param sql
     * @return
     * @throws SQLException
     */
    public static ResultSet querySQL(Connection conn, String sql) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.setQueryTimeout(20);
        return stmt.executeQuery(sql);
    }

    /**
     * 执行数据定义语句
     * @param conn
     * @param sql
     */
    public static void executeDDL(Connection conn, String sql) throws SQLException {
        Statement stmt = conn.createStatement();
        stmt.setQueryTimeout(200);
        stmt.execute(sql);
    }

    /**
     * 获取属性的字符串格式
     * @param s
     * @return
     */
    public static String getDBString(String s) {
        return "'" + s.replaceAll("'","''") + "'";
    }
}
