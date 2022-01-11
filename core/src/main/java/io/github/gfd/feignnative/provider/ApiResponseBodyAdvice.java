package io.github.gfd.feignnative.provider;

import com.alibaba.fastjson.JSON;
import io.github.gfd.feignnative.ApiResponse;
import io.github.gfd.feignnative.FeignApiProperties;
import io.github.gfd.feignnative.anno.ApiResponseController;
import io.github.gfd.feignnative.anno.IgnoreApiResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 把控制器返回结果包装 {@link ApiResponse}
 */
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@ControllerAdvice(annotations = ApiResponseController.class)
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Autowired
    FeignApiProperties feignApiProperties;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !returnType.getParameterType().equals(ApiResponse.class)
                && AnnotatedElementUtils.findMergedAnnotation(returnType.getAnnotatedElement(), IgnoreApiResponse.class) == null;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        if (body instanceof ApiResponse) {
            return body;
        }
        if (returnType.getParameterType().equals(String.class)) {
            return JSON.toJSONString(success(body));
        }
        return success(body);
    }

    protected ApiResponse<Object> success(Object body) {
        ApiResponse<Object> apiResponse = getApiResponse();
        return apiResponse.successData(body);
    }

    protected ApiResponse<Object> getApiResponse() {
        return (ApiResponse<Object>) BeanUtils.instantiateClass(feignApiProperties.getResponseClass());
    }

}
