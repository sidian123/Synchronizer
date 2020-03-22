///*
// * Copyright (C) 2016 alchemystar, Inc. All Rights Reserved.
// */
//package live.sidian.database.synchronizer.utils;
//
//
//
//import live.sidian.database.synchronizer.model.Column;
//import live.sidian.database.synchronizer.model.Index;
//import live.sidian.database.synchronizer.model.MetaData;
//import live.sidian.database.synchronizer.model.Table;
//
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * @Author lizhuyang
// */
//public class CompareUnits {
//
//    private MetaData source;
//    private MetaData target;
//
//    private List<String> changeSql;
//
//    public CompareUnits(MetaData source, MetaData target) {
//        this.source = source;
//        this.target = target;
//        changeSql = new ArrayList<String>();
//    }
//
//    public void compare() {
//        compareSchema();
//        compareTables();
//        compareKeys();
//    }
//
//    private void compareSchema() {
//        // if not exist
//        // changeSql.add("create database if not exists " + SqlUtils.getDBString(source.getSchema())+";");
//        source.init();
//        target.init();
//    }
//
//    private void compareTables() {
//        for (Table table : source.getTables().values()) {
//            if (target.getTables().get(table.getName()) == null) {
//                // 如果对应的target没有这张表,直接把create Table拿出
//                changeSql.add(table.getCreateTableSQL() + ";");
//                continue;
//            }
//            // 这样就需要比较两者的字段
//            compareSingelTable(table, target.getTables().get(table.getName()));
//        }
//
//    }
//
//    private void compareSingelTable(Table sourceTable, Table targetTable) {
//        compareColumns(sourceTable, targetTable);
//    }
//
//    private void compareColumns(Table sourceTable, Table targetTable) {
//        // 记录最后一个比较的column
//        String after = null;
//        for (Column column : sourceTable.getColumns().values()) {
//            if (targetTable.getColumns().get(column.getName()) == null) {
//                // 如果对应的target没有这个字段,直接alter
//                String sql = "alter table " + target.getSchema() + "." + targetTable.getName() + " add " + column
//                        .getName() + " ";
//                sql += column.getType() + " ";
//                if (column.getIsNull().equals("NO")) {
//                    sql += "NOT NULL ";
//                } else {
//                    sql += "NULL ";
//                }
//                if (column.getCollate() != null) {
//                    sql += "COLLATE " + SqlUtils.getDBString(column.getCollate()) + " ";
//                }
//                if (column.getDefaultValue() != null) {
//                    sql += "DEFAULT " + SqlUtils.getDBString(column.getDefaultValue()) + " ";
//                } else {
//                    sql += "DEFAULT NULL ";
//                }
//                if (column.getComment() != null) {
//                    sql += "COMMENT " + SqlUtils.getDBString(column.getComment()) + " ";
//                }
//                if (after != null) {
//                    sql += "after " + after;
//                }
//                changeSql.add(sql + ";");
//            } else {
//                // 检查对应的source 和 target的属性
//                String sql =
//                        "alter table " + target.getSchema() + "." + targetTable.getName() + " change " + column
//                                .getName() + " ";
//                Column sourceColumn = column;
//                Column targetColumn = targetTable.getColumns().get(sourceColumn.getName());
//                // 比较两者字段,如果返回null,表明一致
//                String sqlExtend = compareSingleColumn(sourceColumn, targetColumn);
//                if (sqlExtend != null) {
//                    changeSql.add(sql + sqlExtend + ";");
//                }
//            }
//            after = column.getName();
//        }
//
//        // remove the target redundancy columns
//        for (Column column : targetTable.getColumns().values()) {
//            if (sourceTable.getColumns().get(column.getName()) == null) {
//                // redundancy , so drop it
//                String sql = "alter table " + target.getSchema() + "." + targetTable.getName() + " drop " + column
//                        .getName() + " ";
//                changeSql.add(sql + ";");
//            }
//        }
//    }
//
//    private String compareSingleColumn(Column sourceColumn, Column targetColumn) {
//        if (sourceColumn.equals(targetColumn)) {
//            return null;
//        }
//        String changeSql = "";
//        if (!sourceColumn.getName().equals(targetColumn.getName())) {
//            // never reach here
//            throw new RuntimeException("the bug in this tool");
//        }
//        changeSql += sourceColumn.getName() + " ";
//        changeSql += sourceColumn.getType() + " ";
//        if (sourceColumn.getIsNull().equals("NO")) {
//            changeSql += "NOT NULL ";
//        } else {
//            changeSql += "NULL ";
//        }
//        if (sourceColumn.getCollate() != null) {
//            changeSql += "COLLATE " + SqlUtils.getDBString(sourceColumn.getCollate()) + " ";
//        }
//        if (sourceColumn.getExtra().toUpperCase().indexOf("AUTO_INCREMENT") != -1) {
//            changeSql += "AUTO_INCREMENT ";
//        }
//        if (sourceColumn.getDefaultValue() != null) {
//            changeSql += "DEFAULT " + SqlUtils.getDBString(sourceColumn.getDefaultValue()) + " ";
//        } else {
//            changeSql += "DEFAULT NULL ";
//        }
//        if (sourceColumn.getComment() != null) {
//            changeSql += "COMMENT " + SqlUtils.getDBString(sourceColumn.getComment()) + " ";
//        }
//        return changeSql;
//    }
//
//    // compare the index
//    private void compareKeys() {
//        for (Table table : source.getTables().values()) {
//            // 这样就需要比较两者的索引)
//            if (target.getTables().get(table.getName()) != null) {
//                compareSingleKeys(table, target.getTables().get(table.getName()));
//            }
//        }
//    }
//
//    private void compareSingleKeys(Table sourceTable, Table targetTable) {
//        for (Index index : sourceTable.getIndexes().values()) {
//            String sql = "alter table " + target.getSchema() + "." + targetTable.getName() + " ";
//            if (targetTable.getIndexes().get(index.getName()) == null) {
//                if (index.getName().equals("PRIMARY")) {
//                    sql += "add primary key ";
//                } else {
//                    if (index.getNotUnique().equals("0")) {
//                        sql += "add unique " + index.getName() + " ";
//                    } else {
//                        sql += "add index " + index.getName() + " ";
//                    }
//                }
//                sql += "(`";
//                for (String key : index.getColumns()) {
//                    sql += key.trim() + "`,`";
//                }
//                // 去掉最后一个,`
//                sql = sql.substring(0, sql.length() - 2) + ")";
//                changeSql.add(sql + ";");
//            }
//        }
//        for (Index index : targetTable.getIndexes().values()) {
//            if (sourceTable.getIndexes().get(index.getName()) == null) {
//                // 表明此索引多余
//                String sql = "alter table " + target.getSchema() + "." + targetTable.getName() + " ";
//                if (index.getName().equals("PRIMARY")) {
//                    sql += "drop primary key ";
//                } else {
//                    sql += "drop index " + index.getName();
//                }
//                changeSql.add(sql + ";");
//            }
//        }
//    }
//
//    public MetaData getSource() {
//        return source;
//    }
//
//    public void setSource(MetaData source) {
//        this.source = source;
//    }
//
//    public MetaData getTarget() {
//        return target;
//    }
//
//    public void setTarget(MetaData target) {
//        this.target = target;
//    }
//
//    public List<String> getChangeSql() {
//        return changeSql;
//    }
//
//    public void setChangeSql(List<String> changeSql) {
//        this.changeSql = changeSql;
//    }
//}
