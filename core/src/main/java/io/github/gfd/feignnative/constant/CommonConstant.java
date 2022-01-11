package io.github.gfd.feignnative.constant;

public interface CommonConstant {

    /**
     * 解析生成过程常量
     */
    String PREFIX_ARG = "arg";
    String SUFFIX_PROXY_CONTROLLER = "_ProxyController";

    /**
     * 注解属性名常量
     */
    String ANNOTATION_PROPERTY_NAME_VALUE = "value";
    String ANNOTATION_PROPERTY_NAME_BASE_PACKAGES = "basePackages";

    /**
     * 反射使用常量
     */
    String CLASS_HANDLER_FEIGN = "feign.ReflectiveFeign$FeignInvocationHandler";
    String CLASS_HANDLER_SENTINEL = "com.alibaba.cloud.sentinel.feign.SentinelInvocationHandler";
    String CLASS_METHOD_HANDLER = "feign.SynchronousMethodHandler";

    String FIELD_DISPATCH = "dispatch";
    String FIELD_METADATA = "metadata";
    String FIELD_PROCESSED_METHODS ="processedMethods";
    String FIELD_CONVERTING_EXPANDER_FACTORY = "convertingExpanderFactory";

    String METHOD_PARSE_AND_VALIDATE_METADATA = "parseAndValidateMetadata";
    String METHOD_CREATE_TYPE_DESCRIPTOR = "createTypeDescriptor";
    String METHOD_GET_EXPANDER = "getExpander";
    String METHOD_PUT = "put";


    /**
     * Api响应码
     */
    int CODE_SUCCESS = 200;
}
