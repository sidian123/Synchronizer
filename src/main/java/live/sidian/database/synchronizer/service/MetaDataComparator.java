package live.sidian.database.synchronizer.service;

import live.sidian.database.synchronizer.model.*;
import live.sidian.database.synchronizer.utils.SqlUtils;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 元数据比较器
 * @author sidian
 * @date 2020/3/22 11:10
 */
public class MetaDataComparator {

    private static final String CREATE  ="create";
    private static final String DELETE="delete";
    private static final String MODIFY="modify";
    private static final String AFTER ="after";
    private static final String BEFORE="before";


    /**
     * 开始比较
     */
    public static PatchSQL diff(MetaData source,MetaData target){
        //比较每个表
        return diffTables(target.getSchema(),source.getTables(),target.getTables());
    }

    /**
     * 差分所有表
     */
    private static PatchSQL diffTables(String targetSchema,Map<String, Table> sourceTables, Map<String, Table> targetTables) {
        PatchSQL patchSQL=new PatchSQL();//差异补丁
        HashSet<String> knownsColumn = new HashSet<>();//target中已访问过的表
        //遍历所有表
        sourceTables.forEach((tableName, table) -> {//对于source的每个table
            //检查target是否存在
            Table targetTable=targetTables.get(tableName);
            if(targetTable==null){//不存在
                //添加补丁-建表语句
                patchSQL.getCreatedTables().add(table.getCreateTableSQL());
            }else {//存在
                PatchSQL.ModifiedTable modifiedTableSQL=new PatchSQL.ModifiedTable();
                String targetFullTableName=String.format("`%s`.`%s`",targetSchema,tableName);
                //差分表的所有列
                Map<String, List<String>> columnsPatch = diffColumns(targetFullTableName, table.getColumns(), targetTable.getColumns());
                modifiedTableSQL.getCreatedColumns().addAll(columnsPatch.get(CREATE));
                modifiedTableSQL.getDeletedColumns().addAll(columnsPatch.get(DELETE));
                modifiedTableSQL.getModifiedColumns().addAll(columnsPatch.get(MODIFY));
                modifiedTableSQL.getAfterSQL().addAll(columnsPatch.get(AFTER));
                modifiedTableSQL.getBeforeSQL().addAll(columnsPatch.get(BEFORE));
                //差分表的所有索引
                Map<String, List<String>> indicesPatch = diffIndices(targetFullTableName, table.getIndexes(), targetTable.getIndexes());
                modifiedTableSQL.getCreatedIndices().addAll(indicesPatch.get(CREATE));
                modifiedTableSQL.getDeletedIndices().addAll(indicesPatch.get(DELETE));
                //记录
                if(!modifiedTableSQL.isEmpty()){
                    patchSQL.getModifiedTables().add(modifiedTableSQL);
                }
                knownsColumn.add(targetTable.getName());
            }
        });
        //target删除多余表
        HashSet<String> redundantTables = new HashSet<>(targetTables.keySet());
        redundantTables.removeAll(knownsColumn);
        redundantTables.forEach(table -> patchSQL.getDeletedTables().add(String.format("drop table `%s`.`%s`", targetSchema,table)));

        return patchSQL;
    }

    /**
     * 差分所有索引
     * @param targetFullTableName target完整的表名, 含scheme前缀
     */
    private static Map<String,List<String>> diffIndices(String targetFullTableName, Map<String, Index> sourceIndices, Map<String, Index> targetIndices) {
        Map<String,List<String>> resultPatchSQL=new HashMap<>();//记录生成的补丁
        resultPatchSQL.put(CREATE,new LinkedList<>());
        resultPatchSQL.put(DELETE,new LinkedList<>());
        String sqlHeader= String.format("alter table %s ",targetFullTableName);//下面用到的sql的头部片段
        HashSet<String> knownColumns = new HashSet<>();//记录target已经被访问过的索引

        //遍历source所有的索引
        sourceIndices.forEach((indexName, sourceIndex) ->{//对于source的每个索引
            //检查目标索引是否存在
            Index targetIndex=targetIndices.get(indexName);
            if(targetIndex==null){//不存在
                //添加补丁-新增索引语句
                resultPatchSQL.get(CREATE).add(
                        buildAddIndexSQL(sqlHeader,sourceIndex)
                );
            }else{//存在
                //比较索引
                if(!sourceIndex.equals(targetIndex)){//索引不一致
                    //补丁-删除索引(没有修改的法子,只能删除)
                    resultPatchSQL.get(DELETE).add(
                            buildRemoveIndexSQL(sqlHeader,targetIndex)
                    );
                    //补丁-新增索引
                    resultPatchSQL.get(CREATE).add(
                            buildAddIndexSQL(sqlHeader,sourceIndex)
                    );
                }
                knownColumns.add(targetIndex.getName());
            }
        });
        //删除无用索引
        HashSet<String> redundantColumns=new HashSet<>(targetIndices.keySet());
        redundantColumns.removeAll(knownColumns);
        redundantColumns.forEach(columnName -> resultPatchSQL.get(DELETE).add(
                buildRemoveIndexSQL(sqlHeader,targetIndices.get(columnName))
        ));

        return resultPatchSQL;
    }

    private static String buildAddIndexSQL(String sqlHeader, Index index) {
        if(index.getName().equals("PRIMARY")){//主键
            return String.format(
                    "%s add primary key(%s)",
                    sqlHeader,
                    String.join(",",index.getColumns())
            );
        }else{
            return String.format(
                        "%s add %s %s (%s)",
                        sqlHeader,
                        index.getType()== Index.IndexType.INDEX?"index":"unique",
                        index.getName(),
                        String.join(",",index.getColumns())
            );
        }
    }


    private static String buildRemoveIndexSQL(String sqlHeader, Index index) {
        if(index.getName().equals("PRIMARY")){//是主键
            return String.format("%s drop primary key", sqlHeader);
        }else{
            return String.format("%s drop index %s",sqlHeader,index.getName());
        }
    }


    /**
     * 差分所有列
     * @param targetFullTableName target完整的表名, 含scheme前缀
     */
    private static Map<String,List<String>> diffColumns(String targetFullTableName,Map<String, Column> sourceColumns,Map<String,Column> targetColumns) {
        Map<String,List<String>> resultPatchSQL=new HashMap<>();//记录生成的补丁
        resultPatchSQL.put(CREATE,new LinkedList<>());
        resultPatchSQL.put(DELETE,new LinkedList<>());
        resultPatchSQL.put(MODIFY,new LinkedList<>());
        resultPatchSQL.put(AFTER,new LinkedList<>());
        resultPatchSQL.put(BEFORE,new LinkedList<>());
        AtomicReference<Column> lastColumn=new AtomicReference<>();//source中上一个遍历的列
        HashSet<String> knownColumns = new HashSet<>();//target中已访问过的列
        String sqlHeader= String.format("alter table %s ",targetFullTableName);//下面用到的sql的头部片段

        //遍历source所有列
        sourceColumns.forEach((columnName, sourceColumn) -> {
            //检查目标列中是否存在
            Column targetColumn=targetColumns.get(columnName);
            if(targetColumn==null){//不存在
                //添加补丁-新增列语句
                if(!StringUtils.isEmpty(sourceColumn.getExtra()) ){//新增列有auto_increment约束
                    //先添加无auto_increment约束的列声明
                    try {
                        Column obj = (Column)BeanUtils.cloneBean(sourceColumn);
                        obj.setExtra("");
                        resultPatchSQL.get(CREATE).add(
                                String.format(
                                        "%s add %s %s",
                                        sqlHeader,
                                        buildColumnDefinePartSql(obj),
                                        lastColumn.get() ==null?"":"after "+ lastColumn.get().getName()
                                )
                        );
                    } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                    //添加有auto_increment约束的列修改, 且操作置后
                    resultPatchSQL.get(AFTER).add(
                            String.format(
                                    "%s modify %s %s",
                                    sqlHeader,
                                    buildColumnDefinePartSql(sourceColumn),
                                    lastColumn.get() ==null?"":"after "+ lastColumn.get().getName()
                            )
                    );
                }else{
                    resultPatchSQL.get(CREATE).add(
                            String.format(
                                    "%s add %s %s",
                                    sqlHeader,
                                    buildColumnDefinePartSql(sourceColumn),
                                    lastColumn.get() ==null?"":"after "+ lastColumn.get().getName()
                            )
                    );
                }
            }else {//存在
                //比较约束
                if(!sourceColumn.equals(targetColumn)) {//列不一致
                    //修改约束
                    String sql=String.format(
                            "%s modify %s %s",
                            sqlHeader,
                            buildColumnDefinePartSql(sourceColumn),
                            lastColumn.get() ==null?"":"after "+ lastColumn.get().getName()
                    );
                    if(!sourceColumn.getExtra().equals(targetColumn.getExtra())) {//有auto_increment约束的改动
                        if(targetColumn.getExtra().equals("")){//要新增auto_increment
                            //操作置后
                            resultPatchSQL.get(AFTER).add(sql);
                        }else{//要删除auto_increment
                            //先操作, 后索引
                            resultPatchSQL.get(BEFORE).add(sql);
                        }
                    }else{
                        resultPatchSQL.get(MODIFY).add(sql);
                    }
                }
                knownColumns.add(targetColumn.getName());
            }
            lastColumn.set(sourceColumn);
        });
        //删除target列中多余的字段
        Set<String> redundantColumns = new HashSet<>(targetColumns.keySet());
        redundantColumns.removeAll(knownColumns);
        redundantColumns.forEach(columnName-> {
            if(!StringUtils.isEmpty(targetColumns.get(columnName).getExtra())){//该列有auto_increment约束
                //操作置后
                resultPatchSQL.get(AFTER).add(
                        String.format("%s drop %s",sqlHeader, columnName)
                );
            }else{
                resultPatchSQL.get(DELETE).add(
                        String.format("%s drop %s",sqlHeader, columnName)
                );
            }
        });
        return resultPatchSQL;
    }

    public static String buildColumnDefinePartSql(Column column){
        return String.format(" `%s` %s %s %s %s %s ",
                column.getName(),
                column.getType(),
                column.isNullable()?"null":"not null",
                column.getExtra(),
                column.getDefaultValue()==null?"":"DEFAULT "+SqlUtils.getDBString(column.getDefaultValue()),
                column.getComment()==null?"":"comment "+SqlUtils.getDBString(column.getComment())
        );
    }
}
