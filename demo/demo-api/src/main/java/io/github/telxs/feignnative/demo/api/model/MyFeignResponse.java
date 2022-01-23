package io.github.telxs.feignnative.demo.api.model;

import io.github.gfd.feignnative.FeignResponse;

public class MyFeignResponse<T> extends FeignResponse<T> {

    public static final int CODE_SUCCESS = 0;

    @Override
    public boolean isSuccess() {
        return getCode() == CODE_SUCCESS;
    }

    public static <T> MyFeignResponse<T> success(T data) {
        MyFeignResponse<T> response = new MyFeignResponse<>();
        response.setCode(CODE_SUCCESS);
        response.setData(data);
        return response;
    }
}
