package io.github.gfd.feignnative.consumer;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.gfd.feignnative.MethodMetadataExt;
import io.github.gfd.feignnative.FeignApiProperties;
import io.github.gfd.feignnative.anno.IgnoreApiResponse;
import io.github.gfd.feignnative.constant.CommonConstant;
import io.github.gfd.feignnative.util.TypeUtil;
import feign.Feign;
import feign.MethodMetadata;
import feign.Param;
import feign.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.gfd.feignnative.constant.CommonConstant.*;
import static feign.Util.checkState;
import static java.util.Optional.ofNullable;

/**
 * 自定义contract实现没有注解情况下对方法和参数解析，兼容原始注解使用
 *
 * @author telx
 */
public class ApiContract extends SpringMvcContract {

    public static final Map<String, MethodMetadataExt> METADATA_EXT_MAP = new HashMap<>();

    public static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor
            .valueOf(String.class);

    private final ConversionService conversionService;

    @Autowired
    private FeignApiProperties feignApiProperties;

    public ApiContract() {
        this(Collections.emptyList(), new DefaultConversionService());
    }

    public ApiContract(List<AnnotatedParameterProcessor> parameterProcessors,
                       ConversionService feignConversionService) {
        super(parameterProcessors, feignConversionService);
        this.conversionService = feignConversionService;
    }

    /**
     * 兼容feign 10.6.0之后版本，方法名称变更
     */
    public List<MethodMetadata> parseAndValidateMetadata(Class<?> targetType) {
        List<MethodMetadata> metadataList = reflectParseAndValidateMetadata(targetType);
        afterMethodMetadata(metadataList, targetType);
        return metadataList;
    }

    @Override
    public List<MethodMetadata> parseAndValidatateMetadata(Class<?> targetType) {
        List<MethodMetadata> metadataList = super.parseAndValidatateMetadata(targetType);
        afterMethodMetadata(metadataList, targetType);
        return metadataList;
    }

    protected void afterMethodMetadata(List<MethodMetadata> metadataList, Class<?> targetType) {
        if (CollectionUtil.isEmpty(metadataList)) {
            return;
        }
        METADATA_EXT_MAP.putAll(metadataList.stream().map(MethodMetadataExt::new)
                .collect(Collectors.toMap(methodMetadataExt ->
                        methodMetadataExt.metadata().configKey(), Function.identity())));
        wrapApiResponseType(targetType);
    }

    @Override
    public MethodMetadata parseAndValidateMetadata(Class<?> targetType, Method method) {
        RequestMapping requestMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
        if (requestMapping != null) {
            return super.parseAndValidateMetadata(targetType, method);
        }
        return parseAndValidateMetadataWithNoHttpAnnotation(targetType, method);

    }

    protected MethodMetadata parseAndValidateMetadataWithNoHttpAnnotation(Class<?> targetType, Method method) {
        Field processedMethods = ReflectUtil.getField(getClass().getSuperclass(), FIELD_PROCESSED_METHODS);
        Object processedMethodsValue = ReflectUtil.getFieldValue(this, processedMethods);
        ReflectUtil.invoke(processedMethodsValue, METHOD_PUT, Feign.configKey(targetType, method), method);
        MethodMetadata data = ReflectUtil.newInstance(MethodMetadata.class);
        data.returnType(TypeUtil.resolve(targetType, targetType, method.getGenericReturnType()));
        data.configKey(Feign.configKey(targetType, method));

        processMethodWithNoHttpAnnotation(data, method);

        checkState(data.template().method() != null,
                "Method %s not annotated with HTTP method type (ex. GET, POST)",
                method.getName());

        Class<?>[] parameterTypes = method.getParameterTypes();

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        int count = parameterAnnotations.length;
        for (int i = 0; i < count; i++) {
            if (parameterTypes[i] == Request.Options.class) {
                continue;
            }
            if (parameterTypes[i] == URI.class) {
                data.urlIndex(i);
            }
            if (ArrayUtil.isNotEmpty(parameterAnnotations[i])) {
                boolean isHttpAnnotation = processAnnotationsOnParameter(data, parameterAnnotations[i], i);
                if (!isHttpAnnotation) {
                    addTemplateBody(data, method, i);
                }
            } else {
                processParameterWithNoHttpAnnotation(data, method, i);
            }
        }

        return data;
    }

    protected void processMethodWithNoHttpAnnotation(MethodMetadata data, Method method) {
        // HTTP Method
        data.template().method(Request.HttpMethod.GET);
        // path
        String pathValue = method.getName();
        if (!data.template().path().endsWith("/")) {
            pathValue = "/" + pathValue;
        }
        data.template().uri(pathValue, true);
        data.indexToExpander(new LinkedHashMap<>());
    }

    protected void processParameterWithNoHttpAnnotation(MethodMetadata data, Method method, int paramIndex) {
        MethodParameter methodParameter = new MethodParameter(method, paramIndex);
        methodParameter.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
        if (data.indexToExpander().get(paramIndex) == null) {
            TypeDescriptor typeDescriptor = reflectCreateTypeDescriptor(method, paramIndex);
            if (this.conversionService.canConvert(typeDescriptor, STRING_TYPE_DESCRIPTOR)) {
                Param.Expander expander = reflectGetExpander(typeDescriptor);
                if (expander != null) {
                    // indexToName
                    String paramName = nameParam(data, paramIndex, methodParameter);
                    // templateParameter
                    addTemplateParameter(data, paramName);
                    // indexToExpander
                    data.indexToExpander().put(paramIndex, expander);
                }
            } else {
                addTemplateBody(data, method, paramIndex);
            }
        }
    }

    private void addTemplateBody(MethodMetadata data, Method method, int paramIndex) {
        checkState(data.bodyIndex() == null, "Method has too many Body parameters: %s", method);
        data.bodyIndex(paramIndex);
        data.bodyType(TypeUtil.resolve(method.getDeclaringClass(),
                method.getDeclaringClass(), method.getGenericParameterTypes()[paramIndex]));
        data.template().method(Request.HttpMethod.POST);
    }

    private String nameParam(MethodMetadata data, int paramIndex, MethodParameter methodParameter) {
        String paramName = methodParameter.getParameterName();
        if (StrUtil.isBlank(paramName)) {
            // rename parameter name because can't get from interface class
            paramName = CommonConstant.PREFIX_ARG + paramIndex;
        }
        nameParam(data, paramName, paramIndex);
        return paramName;
    }


    private void addTemplateParameter(MethodMetadata data, String paramName) {
        Collection<String> rest = data.template().queries().get(paramName);
        Collection<String> query = ofNullable(rest).map(ArrayList::new)
                .orElse(new ArrayList<>());
        query.add(String.format("{%s}", paramName));
        data.template().query(paramName, query);
    }

    public List<MethodMetadata> reflectParseAndValidateMetadata(Class<?> targetType) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            MethodHandle parseAndValidateMetadata = lookup.findSpecial(SpringMvcContract.class, METHOD_PARSE_AND_VALIDATE_METADATA,
                    MethodType.methodType(List.class, Class.class), this.getClass());
            return (List<MethodMetadata>) parseAndValidateMetadata.invoke(this, targetType);
        } catch (Throwable e) {
            throw new IllegalStateException("can't reflect invoke super method [parseAndValidateMetadata]", e);
        }
    }

    public TypeDescriptor reflectCreateTypeDescriptor(Method method, int paramIndex) {
        Method createTypeDescriptor = ReflectUtil.getMethodByName(getClass().getSuperclass(), METHOD_CREATE_TYPE_DESCRIPTOR);
        return ReflectUtil.invokeStatic(createTypeDescriptor, method, paramIndex);
    }


    public Param.Expander reflectGetExpander(TypeDescriptor typeDescriptor) {
        Field convertingExpanderFactory = ReflectUtil.getField(getClass().getSuperclass(), FIELD_CONVERTING_EXPANDER_FACTORY);
        Object convertingExpanderFactoryValue = ReflectUtil.getFieldValue(this, convertingExpanderFactory);
        return ReflectUtil.invoke(convertingExpanderFactoryValue, METHOD_GET_EXPANDER, typeDescriptor);
    }

    protected void wrapApiResponseType(Class<?> targetType) {
        IgnoreApiResponse ignoreApiResponseOnClass = AnnotationUtils.findAnnotation(targetType, IgnoreApiResponse.class);
        for (Method method : targetType.getMethods()) {
            MethodMetadataExt metadataExt = METADATA_EXT_MAP.get(Feign.configKey(targetType, method));
            if (metadataExt == null || ignoreApiResponseOnClass != null) {
                continue;
            }
            if (AnnotationUtils.findAnnotation(method, IgnoreApiResponse.class) != null) {
                continue;
            }
            MethodMetadata metadata = metadataExt.metadata();
            Type returnType = metadata.returnType();
            if (TypeUtil.isEqualsClassAny(returnType, feignApiProperties.getResponseClass(), Future.class)) {
                continue;
            }
            metadata.returnType(new ApiResponseType(returnType, feignApiProperties.getResponseClass()));
        }
    }

    static class ApiResponseType implements ParameterizedType {

        private final Type originType;

        private final Type rawType;

        public ApiResponseType(Type originType, Type rawType) {
            this.originType = originType;
            this.rawType = rawType;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{originType};
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }

}
