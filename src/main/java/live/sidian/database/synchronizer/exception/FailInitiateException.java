package live.sidian.database.synchronizer.exception;

import java.sql.SQLException;

/**
 * @author sidian
 * @date 2020/3/21 23:17
 */
public class FailInitiateException extends Throwable {
    public FailInitiateException(String s, Throwable e) {
        super(s,e);
    }
}
