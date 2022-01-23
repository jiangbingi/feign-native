package io.github.telxs.feignnative.demo.api;

import io.github.telxs.feignnative.demo.api.model.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "demo-provider", path = "/api", url = "http://localhost:8081")
public interface DemoApi {

    /**
     * 简单类型(RequestParam)
     * 不加注解接口上拿不到参数的名称，所以会重命名为arg0
     */
    String sayHello(String name);

    /**
     * POJO(RequestBody) + 简单类型(RequestParam，名名为arg1)
     */
    OrderDTO queryOrder(OrderDTO orderDTO, Long uid);

    /**
     *  POJO(RequestParam) + 简单类型(RequestParam，重命名为arg1)
     *
     */
    OrderDTO queryOrderByParamAnnotation(@RequestBody OrderDTO orderDTO, Long uid);

    /**
     * 自定义路径 + POJO(RequestBody) + 简单类型(RequestParam)
     *
     * 使用@RequestMapping按原来feign处理流程，如果不加注解都按body处理会报错，只能有一个body
     */
    @RequestMapping("/customQueryOrder")
    OrderDTO queryOrderByMethodAnnotation(OrderDTO orderDTO, @RequestParam("uid") Long uid);


    OrderDTO testFeignBusinessException(Long uid);

}
