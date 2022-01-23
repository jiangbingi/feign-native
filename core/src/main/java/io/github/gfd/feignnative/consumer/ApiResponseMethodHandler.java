package io.github.gfd.feignnative.consumer;

import cn.hutool.core.util.ReflectUtil;
import io.github.gfd.feignnative.ApiResponse;
import io.github.gfd.feignnative.FeignBusinessException;
import io.github.gfd.feignnative.MethodMetadataExt;
import io.github.gfd.feignnative.util.TypeUtil;
import feign.InvocationHandlerFactory;
import feign.MethodMetadata;

import static io.github.gfd.feignnative.constant.CommonConstant.CLASS_METHOD_HANDLER;
import static io.github.gfd.feignnative.constant.CommonConstant.FIELD_METADATA;


/**
 * 对应Api结果进行判断，失败抛出 {@link FeignBusinessException}
 */
public class ApiResponseMethodHandler implements InvocationHandlerFactory.MethodHandler {

    private final InvocationHandlerFactory.MethodHandler delegate;

    private MethodMetadata cache;

    ApiResponseMethodHandler(InvocationHandlerFactory.MethodHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object[] argv) throws Throwable {
        Object result = delegate.invoke(argv);
        if (cache == null && CLASS_METHOD_HANDLER.equals(delegate.getClass().getName())) {
            Object metadata = ReflectUtil.getFieldValue(delegate, FIELD_METADATA);
            cache = (MethodMetadata) metadata;
        }
        if (cache != null) {
            MethodMetadataExt metadataExt = ApiContract.METADATA_EXT_MAP.get(cache.configKey());
            if (result instanceof ApiResponse
                    && !TypeUtil.isEqualsClassAny(metadataExt.originReturnType(), ApiResponse.class)) {
                ApiResponse<?> apiResponse = (ApiResponse<?>) result;
                if (!apiResponse.isSuccess()) {
                    throw new FeignBusinessException(apiResponse.getCode(), apiResponse.getMessage());
                }
                return apiResponse.getData();
            }
        }
        return result;
    }
}
