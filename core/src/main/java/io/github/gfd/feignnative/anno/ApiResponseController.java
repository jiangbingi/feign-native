package io.github.gfd.feignnative.anno;

import io.github.gfd.feignnative.provider.FeignControllerRegistrar;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * feign接口动态生成控制器标识，区别于正常的控制器
 * @see FeignControllerRegistrar
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RestController
public @interface ApiResponseController {

    @AliasFor(annotation = RestController.class)
    String value() default "";
}
