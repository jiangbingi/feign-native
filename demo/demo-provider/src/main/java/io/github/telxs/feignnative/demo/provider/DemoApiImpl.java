package io.github.telxs.feignnative.demo.provider;

import io.github.gfd.feignnative.FeignBusinessException;
import io.github.telxs.feignnative.demo.api.DemoApi;
import io.github.telxs.feignnative.demo.api.model.OrderDTO;
import org.springframework.stereotype.Service;

@Service
public class DemoApiImpl implements DemoApi {

    @Override
    public String sayHello(String name) {
        return "hello " + name;
    }

    @Override
    public OrderDTO queryOrder(OrderDTO orderDTO, Long uid) {
        return createOrderDTO(orderDTO, uid);
    }

    @Override
    public OrderDTO queryOrderByMethodAnnotation(OrderDTO orderDTO, Long uid) {
        return createOrderDTO(orderDTO, uid);
    }

    @Override
    public OrderDTO queryOrderByParamAnnotation(OrderDTO orderDTO, Long uid) {
        return createOrderDTO(orderDTO, uid);
    }

    @Override
    public OrderDTO testFeignBusinessException(Long uid) {
        return createOrderDTO(new OrderDTO(), uid);
    }

    private OrderDTO createOrderDTO(OrderDTO orderDTO, Long uid) {
        if (uid == 0){
            throw new FeignBusinessException(400, "uid异常");
        }
        OrderDTO result = new OrderDTO();
        result.setOrderNo(orderDTO.getOrderNo());
        result.setAmount(1000);
        result.setUid(uid);
        return result;
    }
}
