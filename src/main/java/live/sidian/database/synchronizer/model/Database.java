package live.sidian.database.synchronizer.model;

import lombok.*;

/**
 * 记录连接数据库的配置信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Database {

    /**
     * 配置名
     */
    private String name;
    /**
     * 数据库主机地址
     */
    private String host;
    /**
     * 端口
     */
    private String port;

    /**
     * 登录用户
     */
    private String user;

    /**
     * 登录密码
     */
    private String password;

    /**
     * 字符编码
     */
    private String charset;

    /**
     * 库名
     */
    private String schema;

}
