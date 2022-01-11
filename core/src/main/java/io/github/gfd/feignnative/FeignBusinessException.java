package io.github.gfd.feignnative;

import feign.FeignException;


/**
 * Feign业务异常，通过 {@link ApiResponse} 响应码判断
 */
public class FeignBusinessException extends FeignException {

    public FeignBusinessException(int status, String message) {
        super(status, message);
    }

    public FeignBusinessException(int status, String message, Throwable cause) {
        super(status, message, cause);
    }
}
