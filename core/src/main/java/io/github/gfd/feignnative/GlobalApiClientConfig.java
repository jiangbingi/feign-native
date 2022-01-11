package io.github.gfd.feignnative;

import io.github.gfd.feignnative.consumer.ApiContract;
import feign.Contract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局feign配置，统一使用 {@link ApiContract}
 */
@Configuration
public class GlobalApiClientConfig {

    @Autowired(required = false)
    private List<AnnotatedParameterProcessor> parameterProcessors = new ArrayList<>();

    @Bean
    @ConditionalOnMissingBean
    public Contract contract(ConversionService feignConversionService){
        return new ApiContract(parameterProcessors, feignConversionService);
    }

}
