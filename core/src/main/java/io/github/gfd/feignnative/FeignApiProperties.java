package io.github.gfd.feignnative;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties("feign.api")
public class FeignApiProperties {

    private Class<?> responseClass = ApiResponse.Default.class;

    public Class<?> getResponseClass() {
        return responseClass;
    }

    public void setResponseClass(Class<?> responseClass) {
        this.responseClass = responseClass;
    }
}
