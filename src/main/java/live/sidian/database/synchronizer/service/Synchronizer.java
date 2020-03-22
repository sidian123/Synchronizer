package live.sidian.database.synchronizer.service;

import live.sidian.database.synchronizer.exception.FailInitiateException;
import live.sidian.database.synchronizer.exception.NotExpectedException;
import live.sidian.database.synchronizer.model.Database;
import live.sidian.database.synchronizer.model.MetaData;
import live.sidian.database.synchronizer.model.PatchSQL;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

/**
 * @author sidian
 * @date 2020/3/21 15:22
 */
@Service
public class Synchronizer {
    @Autowired
    MetaDataService metaDataService;

    /**
     * 同步结构
     */
    public void sync(Database source,Database target) throws FailInitiateException {
        //参数校验
        validate(source);
        validate(target);
        //数据库元数据初始化
        MetaData sourceMetaData=initMetaData(source);
        MetaData targetMetaData=initMetaData(target);
        //比较
        PatchSQL diff = MetaDataComparator.diff(sourceMetaData, targetMetaData);
        //打印
        System.out.println(diff.print());
    }

    /**
     * 数据库元数据初始化
     */
    private MetaData initMetaData(Database database) throws FailInitiateException {
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
            metaDataService.init(metaData);
        } catch (SQLException e) {
            throw new FailInitiateException("数据库元数据初始化失败",e);
        }
        return metaData;
    }

    private void validate(Database database) {
        //获取所有属性
        PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(database.getClass());
        //遍历
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            try {
                String propertyName=propertyDescriptor.getName();
                //获取属性值
                String propertyValue= (String) PropertyUtils.getSimpleProperty(database, propertyName);
                //验证
                if(StringUtils.isBlank(propertyValue)){
                    throw new IllegalArgumentException(propertyName+"is missing");
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new NotExpectedException("不在预料之内的异常");
            }
        }
    }

}
