package io.github.gfd.feignnative.consumer;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import feign.InvocationHandlerFactory;
import io.github.gfd.feignnative.constant.CommonConstant;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.openfeign.FeignClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;


/**
 * 获取feign动态代理handler，拦截并修改自定义的MethodHandler {@link ApiResponseMethodHandler}
 * 因为可见性原因通过类名判断
 */
public class FeignHandlerBeanPostProcessor implements BeanPostProcessor {


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        if (!Proxy.isProxyClass(beanClass)) {
            return bean;
        }
        TargetSource targetSource = null;
        Object target = bean;
        if (bean instanceof Advised) {
            Advised advised = (Advised) bean;
            Class<?>[] proxiedInterfaces = advised.getProxiedInterfaces();
            for (Class<?> proxiedInterface : proxiedInterfaces) {
                if (proxiedInterface.getAnnotation(FeignClient.class) != null) {
                    targetSource = advised.getTargetSource();
                    try {
                        target = targetSource.getTarget();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        InvocationHandler targetHandler = Proxy.getInvocationHandler(target);
        if (targetSource == null && !isFeignHandler(targetHandler.getClass().getName())) {
            return bean;
        }
        if (isFeignHandler(targetHandler.getClass().getName())) {
            // delegate MethodHandler
            Object value = ReflectUtil.getFieldValue(targetHandler, CommonConstant.FIELD_DISPATCH);
            if (value instanceof Map) {
                Map<Method, InvocationHandlerFactory.MethodHandler> dispatch = (Map<Method, InvocationHandlerFactory.MethodHandler>) value;
                for (Map.Entry<Method, InvocationHandlerFactory.MethodHandler> entry : dispatch.entrySet()) {
                    dispatch.put(entry.getKey(), new ApiResponseMethodHandler(entry.getValue()));
                }
            }
        }
        return bean;
    }

    private boolean isFeignHandler(String name) {
        return StrUtil.equalsAny(name, CommonConstant.CLASS_HANDLER_FEIGN, CommonConstant.CLASS_HANDLER_SENTINEL);
    }

}
