package io.github.gfd.feignnative.anno;

import io.github.gfd.feignnative.GlobalApiClientConfig;
import io.github.gfd.feignnative.consumer.FeignHandlerBeanPostProcessor;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 提供Consumer端支持，主要实现没有注解情况下进行参数解析传递和结果封装判断
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({FeignHandlerBeanPostProcessor.class, GlobalApiClientConfig.class})
public @interface EnableFeignClientsNative {

}
