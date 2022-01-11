package io.github.telxs.feignnative.demo.consumer;

import io.github.telxs.feignnative.demo.api.DemoApi;
import io.github.telxs.feignnative.demo.api.model.FeignResponse;
import io.github.telxs.feignnative.demo.api.model.OrderDTO;
import io.github.gfd.feignnative.anno.EnableFeignClientsNative;
import feign.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableFeignClientsNative
@EnableFeignClients(basePackages = "io.github.telxs.feignnative.demo.api")
@SpringBootApplication
public class ConsumerApplication {

    @Autowired
    DemoApi demoApi;

    @RequestMapping("/sayHello")
    public FeignResponse<String> sayHello(String name) {
        return FeignResponse.success(demoApi.sayHello(name));
    }

    @RequestMapping("/queryOrder")
    public FeignResponse<OrderDTO> queryOrder(@RequestBody OrderDTO orderDTO) {
        return FeignResponse.success(demoApi.queryOrder(orderDTO, orderDTO.getUid()));
    }

    @RequestMapping("/queryOrderByRequestMapping")
    public FeignResponse<OrderDTO> queryOrderByRequestMapping(@RequestBody OrderDTO orderDTO) {
        return FeignResponse.success(demoApi.queryOrderByMethodAnnotation(orderDTO, orderDTO.getUid()));
    }

    @RequestMapping("/queryOrderByParamAnnotation")
    public FeignResponse<OrderDTO> queryOrderByParamAnnotation(@RequestBody OrderDTO orderDTO) {
        return FeignResponse.success(demoApi.queryOrderByParamAnnotation(orderDTO, orderDTO.getUid()));
    }

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }

    @Bean
    Logger.Level feignLoggerLevel(){
        return Logger.Level.FULL;
    }

}
