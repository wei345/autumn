package io.liuwei.autumn.exception;

/**
 * @author liuwei
 * @since 2021-07-08 17:53
 */
public class RememberMeParsingException extends RuntimeException {
    public RememberMeParsingException() {
    }

    public RememberMeParsingException(String message) {
        super(message);
    }

    public RememberMeParsingException(Throwable cause) {
        super(cause);
    }

    public RememberMeParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
