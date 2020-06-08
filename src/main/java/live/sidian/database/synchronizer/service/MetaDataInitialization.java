package live.sidian.database.synchronizer.service;

import live.sidian.database.synchronizer.exception.FailInitiateException;
import live.sidian.database.synchronizer.model.*;
import live.sidian.database.synchronizer.utils.SqlUtils;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * @author sidian
 * @date 2020/3/21 20:15
 */
@CommonsLog
@Service
public class MetaDataInitialization {

    /**
     * 数据库元数据初始化
     */
    MetaData initMetaData(Database database) throws FailInitiateException {
        //装配对象
        MetaData metaData = MetaData.builder()
                .jdbcUrl(
                        String.format("jdbc:mysql://%s/%s?characterEncoding=%s&serverTimezone=GMT%%2B8",
                                database.getHost(), database.getSchema(), database.getCharset())
                )
                .user(database.getUser())
                .password(database.getPassword())
                .schema(database.getSchema())
                .build();
        //初始化
        try {
            doInit(metaData);
        } catch (SQLException e) {
            throw new FailInitiateException("数据库元数据初始化失败", e);
        } finally {
            //释放资源
            if (metaData.getConnection() != null) {
                try {
                    metaData.getConnection().close();
                } catch (SQLException e) {
                    log.warn("链接关闭失败", e);
                }
            }
        }
        return metaData;
    }

    private void doInit(MetaData metaData) throws SQLException {
        //连接数据库
        metaData.setConnection(
                DriverManager.getConnection(
                        metaData.getJdbcUrl(),
                        metaData.getUser(),
                        metaData.getPassword())
        );
        //初始化表信息
        initTablesData(metaData);
        //初始化列信息
        initColumnsData(metaData);
        //初始化索引
        initIndicesData(metaData);
    }

    private void initIndicesData(MetaData metaData) throws SQLException {
        for(Table table:metaData.getTables().values()){//对于每张表
            //查询表的所有索引信息
            ResultSet rs = SqlUtils.querySQL(
                    metaData.getConnection(),
                    String.format("show keys from %s.%s ", metaData.getSchema(),table.getName())
            );
            //遍历所有索引
            while (rs.next()) {//对于每个索引
                //获取索引名
                String keyName = rs.getString("Key_name");
                //判断是否已记录该索引
                Index index = table.getIndexes().get(keyName);
                if(index==null){//不存在
                    //新增索引
                    index = Index.builder()
                            .name(keyName)
                            .columns(new ArrayList<>(Collections.singletonList(rs.getString("Column_name"))))
                            .type(rs.getInt("Non_unique") == 0 ? Index.IndexType.UNIQUE : Index.IndexType.INDEX)
                            .build();
                    table.getIndexes().put(index.getName(),index);
                }else{//存在
                    //新增索引对应的列
                    index.getColumns().add(rs.getString("Column_name"));
                }
            }
        }
    }

    private void initColumnsData(MetaData metaData) throws SQLException {
        for(Table table:metaData.getTables().values()){
            ResultSet rs = SqlUtils.querySQL(
                    metaData.getConnection(),
                    String.format(
                            "select COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COLUMN_DEFAULT,COLUMN_COMMENT,EXTRA"
                            + " from information_schema.columns "
                            + "where TABLE_SCHEMA='%s' and TABLE_NAME='%s' order by ORDINAL_POSITION asc",
                            metaData.getSchema(),
                            table.getName()
                    )
            );
            while (rs.next()) {
                Column column = new Column();
                column.setName(rs.getString("COLUMN_NAME"));
                column.setType(rs.getString("COLUMN_TYPE"));
                column.setDefaultValue(rs.getString("COLUMN_DEFAULT"));
                column.setNullable(rs.getString("IS_NULLABLE").equals("YES"));
                column.setExtra(rs.getString("EXTRA"));
                column.setComment(rs.getString("COLUMN_COMMENT"));
                table.getColumns().put(column.getName(), column);
            }
        }
    }

    private void initTablesData(MetaData metaData) throws SQLException {
        Map<String, Table> tables = metaData.getTables();
        //获取所有表名
        ResultSet rs = SqlUtils.querySQL(metaData.getConnection(), String.format("show tables from %s",metaData.getSchema()));
        while (rs.next()) {
            Table table = Table.builder()
                    .name(rs.getString("Tables_in_" + metaData.getSchema()))
                    .build();
            tables.put(table.getName(),table);
        }
        //获取建表语句
        for(Table table:tables.values()){
            rs = SqlUtils.querySQL(metaData.getConnection(), String.format("show create table %s.%s", metaData.getSchema(),table.getName()));
            while (rs.next()) {
                table.setCreateTableSQL(rs.getString("Create Table"));
            }
        }
    }
}
