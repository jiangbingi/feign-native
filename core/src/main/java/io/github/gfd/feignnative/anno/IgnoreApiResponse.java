package io.github.gfd.feignnative.anno;


import io.github.gfd.feignnative.ApiResponse;
import io.github.gfd.feignnative.consumer.ApiContract;
import io.github.gfd.feignnative.provider.FeignControllerRegistrar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标识是否忽略包装成 {@link ApiResponse}
 * @see FeignControllerRegistrar
 * @see ApiContract
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreApiResponse {
}
