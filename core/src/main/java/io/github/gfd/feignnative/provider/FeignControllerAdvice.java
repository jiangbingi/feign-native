package io.github.gfd.feignnative.provider;

import io.github.gfd.feignnative.FeignBusinessException;
import io.github.gfd.feignnative.FeignResponse;
import io.github.gfd.feignnative.anno.ApiResponseController;
import io.github.gfd.feignnative.constant.CommonConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(annotations = ApiResponseController.class)
public class FeignControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(FeignControllerAdvice.class);

    @ExceptionHandler(FeignBusinessException.class)
    public FeignResponse<?> handleFeignBusinessException(FeignBusinessException ex) {
        logger.warn(ex.getMessage(), ex);
        FeignResponse<?> response = new FeignResponse<>();
        response.setCode(ex.status());
        response.setMessage(ex.getMessage());
        return response;
    }

    @ExceptionHandler(Exception.class)
    public FeignResponse<?> handleException(Exception ex) {
        logger.error(ex.getMessage(), ex);
        FeignResponse<?> response = new FeignResponse<>();
        response.setCode(CommonConstant.CODE_COMMON_ERROR);
        response.setMessage(ex.getMessage());
        return response;
    }
}
