package live.sidian.database.synchronizer.exception;

/**
 * 不被预料的异常, 暗示着程序有问题
 * @author sidian
 * @date 2020/3/21 15:59
 */
public class NotExpectedException extends RuntimeException {
    public NotExpectedException(String s) {
        super(s);
    }
}
