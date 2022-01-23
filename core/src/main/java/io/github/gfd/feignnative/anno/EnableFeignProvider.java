package io.github.gfd.feignnative.anno;

import io.github.gfd.feignnative.provider.ApiResponseBodyAdvice;
import io.github.gfd.feignnative.provider.FeignControllerAdvice;
import io.github.gfd.feignnative.provider.FeignControllerRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 提供Provider端支持，主要动态生成控制器，暴露HTTP接口以及包装控制器返回值
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({FeignControllerRegistrar.class, ApiResponseBodyAdvice.class, FeignControllerAdvice.class})
public @interface EnableFeignProvider {

    String[] basePackages() default {};
}
