package hospital.service;

/**
 * 业务异常：message 为可直接展示给用户的中文提示。
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
