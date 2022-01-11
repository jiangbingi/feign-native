package io.github.gfd.feignnative.provider;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import io.github.gfd.feignnative.CombinedAnnotationMethodParameter;
import io.github.gfd.feignnative.anno.ApiResponseController;
import io.github.gfd.feignnative.anno.EnableFeignProvider;
import io.github.gfd.feignnative.constant.CommonConstant;
import io.github.gfd.feignnative.constant.DebugConstant;
import io.github.gfd.feignnative.util.MethodUtil;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.jar.asm.Opcodes;
import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.gfd.feignnative.constant.CommonConstant.*;
import static feign.Util.checkState;


/**
 * 对标注 {@link Service} 注解的实现类生成对应的控制器，暴露HTTP接口
 */
public class FeignControllerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {

    private static final String debugLocation = System.getProperty(DebugConstant.CLASS_LOCATION);

    private static final List<Class<? extends Annotation>> HTTP_PARAM_ANNOTATION =
            ListUtil.toList(RequestParam.class, RequestBody.class, PathVariable.class);

    private ResourceLoader resourceLoader;

    private Environment environment;

    private final BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

    private final ConversionService conversionService = new DefaultFormattingConversionService();

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        Set<String> basePackages = getBasePackages(metadata);

        Set<AbstractBeanDefinition> serviceBeanDefs = scanService(basePackages, registry);

        registerServiceController(serviceBeanDefs, registry);

    }

    private void registerServiceController(Set<AbstractBeanDefinition> serviceBeanDefs, BeanDefinitionRegistry registry) {
        for (AbstractBeanDefinition serviceBeanDef : serviceBeanDefs) {
            Class<?> beanClass = ClassUtils.resolveClassName(serviceBeanDef.getBeanClassName(), ClassUtils.getDefaultClassLoader());
            Class<?>[] allInterfaces = ClassUtils.getAllInterfacesForClass(beanClass);
            for (Class<?> interfaceClass : allInterfaces) {
                FeignClient annotation = AnnotationUtils.getAnnotation(interfaceClass, FeignClient.class);
                if (annotation != null) {
                    checkState(interfaceClass.getTypeParameters().length == 0,
                            "Parameterized types unsupported: %s", interfaceClass.getSimpleName());
                    checkState(interfaceClass.getInterfaces().length <= 1,
                            "Only single inheritance supported: %s", interfaceClass.getSimpleName());
                    if (interfaceClass.getInterfaces().length == 1) {
                        checkState(interfaceClass.getInterfaces()[0].getInterfaces().length == 0,
                                "Only single-level inheritance supported: %s", interfaceClass.getSimpleName());
                    }

                    DynamicType.Builder<?> builder = new ByteBuddy().subclass(beanClass)
                            .name(beanClass.getName() + SUFFIX_PROXY_CONTROLLER)
                            .annotateType(AnnotationDescription.Builder.ofType(ApiResponseController.class).build())
                            .annotateType(AnnotationDescription.Builder.ofType(RequestMapping.class)
                                    .defineArray(ANNOTATION_PROPERTY_NAME_VALUE, new String[]{annotation.path()})
                                    .build());
                    DynamicType.Builder.MethodDefinition<?> bodyDefinition = null;

                    for (Method method : beanClass.getMethods()) {
                        if (method.isBridge() || Modifier.isFinal(method.getModifiers())
                                || Modifier.isStatic(method.getModifiers())
                                || !isOverriddenFrom(method, interfaceClass)) {
                            continue;
                        }
                        DynamicType.Builder.MethodDefinition<?> current = null;
                        // handle method
                        if (AnnotationUtils.findAnnotation(method, RequestMapping.class) == null) {
                            current = addRequestMappingForMethod(method, beanClass, builder, bodyDefinition, current);
                            bodyDefinition = current;
                        }
                        // handle parameter
                        boolean requestBody = false;
                        Parameter[] parameters = method.getParameters();
                        for (int i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
                            CombinedAnnotationMethodParameter methodParameter = new CombinedAnnotationMethodParameter(method, i);
                            if (!findHttpParamAnnotation(methodParameter)) {
                                if (conversionService.canConvert(methodParameter.getParameterType(), String.class)) {
                                    current = addRequestParamForParameter(methodParameter, beanClass, builder, bodyDefinition, current);
                                } else {
                                    checkState(!requestBody, "Method has too many Body parameters: %s", method);
                                    current = addRequestBodyForParameter(methodParameter, beanClass, builder, bodyDefinition, current);
                                    requestBody = true;
                                }
                                bodyDefinition = current;
                            }
                        }
                    }
                    DynamicType.Unloaded<?> unloaded;
                    if (bodyDefinition != null) {
                        unloaded = bodyDefinition.make();
                    } else {
                        unloaded = builder.make();
                    }
                    Class<?> proxyBeanClass = unloaded
                            .load(ClassUtils.getDefaultClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                            .getLoaded();
                    serviceBeanDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, true);
                    serviceBeanDef.setBeanClass(proxyBeanClass);
                    if (StrUtil.isNotBlank(debugLocation)) {
                        outputClazz(unloaded.getBytes(), proxyBeanClass);
                    }
                    break;
                }
            }
        }
    }

    private boolean isOverriddenFrom(Method method, Class<?> type) {
        while (true) {
            for (Method candidate : type.getMethods()) {
                if (MethodUtil.isOverriddenFrom(method, candidate)) {
                    return true;
                }
            }
            if (type.getInterfaces().length == 0) {
                break;
            }
            type = type.getInterfaces()[0];
        }
        return false;
    }

    private DynamicType.Builder.MethodDefinition<?> addRequestMappingForMethod(Method method, Class<?> beanClass, DynamicType.Builder<?> builder,
                                                                               DynamicType.Builder.MethodDefinition<?> bodyDefinition,
                                                                               DynamicType.Builder.MethodDefinition<?> current) {
        current = getCurrentDefinition(method, beanClass, builder, bodyDefinition, current);
        return current
                .annotateMethod(AnnotationDescription.Builder.ofType(RequestMapping.class)
                        .defineArray(ANNOTATION_PROPERTY_NAME_VALUE, new String[]{method.getName()})
                        .build());
    }

    private DynamicType.Builder.MethodDefinition<?> addRequestParamForParameter(MethodParameter methodParameter,
                                                                                Class<?> beanClass, DynamicType.Builder<?> builder,
                                                                                DynamicType.Builder.MethodDefinition<?> bodyDefinition,
                                                                                DynamicType.Builder.MethodDefinition<?> current) {
        current = getCurrentDefinition(methodParameter.getMethod(), beanClass, builder, bodyDefinition, current);
        int parameterIndex = methodParameter.getParameterIndex();
        return current.
                annotateParameter(parameterIndex, AnnotationDescription.Builder.ofType(RequestParam.class)
                        .define(ANNOTATION_PROPERTY_NAME_VALUE, CommonConstant.PREFIX_ARG + parameterIndex).build());
    }

    private DynamicType.Builder.MethodDefinition<?> addRequestBodyForParameter(MethodParameter methodParameter,
                                                                               Class<?> beanClass, DynamicType.Builder<?> builder,
                                                                               DynamicType.Builder.MethodDefinition<?> bodyDefinition,
                                                                               DynamicType.Builder.MethodDefinition<?> current) {
        current = getCurrentDefinition(methodParameter.getMethod(), beanClass, builder, bodyDefinition, current);
        int parameterIndex = methodParameter.getParameterIndex();
        return current.annotateParameter(parameterIndex,
                AnnotationDescription.Builder.ofType(RequestBody.class).build());

    }

    private DynamicType.Builder.MethodDefinition<?> getCurrentDefinition(Method method, Class<?> beanClass, DynamicType.Builder<?> builder,
                                                                         DynamicType.Builder.MethodDefinition<?> bodyDefinition,
                                                                         DynamicType.Builder.MethodDefinition<?> current) {
        if (bodyDefinition == null && current == null) {
            bodyDefinition = createMethodDefinition(method, beanClass, builder);
            current = bodyDefinition;
        } else if (current == null) {
            current = createMethodDefinition(method, beanClass, bodyDefinition);
        }
        return current;
    }

    private DynamicType.Builder.MethodDefinition<?> createMethodDefinition(Method method, Class<?> beanClass, DynamicType.Builder<?> builder) {
        Type returnType = GenericTypeResolver.resolveType(method.getGenericReturnType(), beanClass);
        DynamicType.Builder.MethodDefinition.ParameterDefinition.Initial<?> initial =
                builder.defineMethod(method.getName(), returnType, Opcodes.ACC_PUBLIC);
        DynamicType.Builder.MethodDefinition.ParameterDefinition.Annotatable<?> annotatable = null;
        Parameter[] parameters = method.getParameters();
        for (int i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
            MethodParameter methodParameter = new MethodParameter(method, i);
            methodParameter.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
            String parameterName = methodParameter.getParameterName();
            Type parameterType = GenericTypeResolver.resolveType(methodParameter.getGenericParameterType(), beanClass);
            if (StrUtil.isBlank(parameterName)) {
                parameterName = CommonConstant.PREFIX_ARG + i;
            }
            if (annotatable == null) {
                annotatable = initial
                        .withParameter(parameterType, parameterName);
            } else {
                annotatable = annotatable
                        .withParameter(parameterType, parameterName);
            }
        }
        DynamicType.Builder.MethodDefinition<?> methodDefinition;
        if (annotatable != null) {
            methodDefinition = annotatable
                    .throwing(method.getExceptionTypes())
                    .intercept(SuperMethodCall.INSTANCE);
        } else {
            methodDefinition = initial
                    .throwing(method.getExceptionTypes())
                    .intercept(SuperMethodCall.INSTANCE);
        }
        return methodDefinition;

    }

    private boolean findHttpParamAnnotation(MethodParameter methodParameter) {
        boolean findHttpParamAnnotation = false;
        Annotation[] parameterAnnotations = methodParameter.getParameterAnnotations();
        if (ArrayUtil.isEmpty(parameterAnnotations)) {
            return false;
        }
        for (Annotation paramAnnotation : methodParameter.getParameterAnnotations()) {
            if (HTTP_PARAM_ANNOTATION.contains(paramAnnotation.annotationType())) {
                findHttpParamAnnotation = true;
                break;
            }
        }
        return findHttpParamAnnotation;
    }

    private Set<AbstractBeanDefinition> scanService(Set<String> basePackages, BeanDefinitionRegistry registry) {
        Set<AbstractBeanDefinition> serviceBeanDefs = new HashSet<>();
        ClassPathScanningCandidateComponentProvider scanner = getScanner();
        scanner.setResourceLoader(this.resourceLoader);
        AnnotationTypeFilter serviceTypeFilter = new AnnotationTypeFilter(Service.class);
        scanner.addIncludeFilter(serviceTypeFilter);
        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition candidateComponent : candidateComponents) {
                BeanDefinition beanDefinition = registry.getBeanDefinition(beanNameGenerator.generateBeanName(candidateComponent, registry));
                if (beanDefinition instanceof AbstractBeanDefinition) {
                    serviceBeanDefs.add((AbstractBeanDefinition) beanDefinition);
                }
            }
        }
        return serviceBeanDefs;
    }

    protected ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false, this.environment) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                boolean isCandidate = false;
                if (beanDefinition.getMetadata().isIndependent()) {
                    if (!beanDefinition.getMetadata().isAnnotation()) {
                        isCandidate = true;
                    }
                }
                return isCandidate;
            }
        };
    }


    protected Set<String> getBasePackages(AnnotationMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(EnableFeignProvider.class.getCanonicalName());

        Set<String> basePackages = new HashSet<>();
        for (String pkg : (String[]) attributes.get(ANNOTATION_PROPERTY_NAME_BASE_PACKAGES)) {
            if (StrUtil.isNotBlank(pkg)) {
                basePackages.add(pkg);
            }
        }
        if (basePackages.isEmpty()) {
            basePackages.add(ClassUtils.getPackageName(metadata.getClassName()));
        }
        return basePackages;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }


    protected void outputClazz(byte[] bytes, Class<?> proxyBeanClass) {
        String dirs = proxyBeanClass.getName().replace('.', File.separatorChar);
        FileUtil.mkParentDirs(debugLocation + File.separator + dirs);
        FileUtil.writeBytes(bytes, debugLocation + File.separator + dirs + ".class");
    }


}
