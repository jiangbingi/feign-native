package io.github.gfd.feignnative;

import io.github.gfd.feignnative.constant.CommonConstant;

public class FeignResponse<T> implements ApiResponse<T>{

    private int code;

    private String message;

    private T data;

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public T getData() {
        return data;
    }

    @Override
    public void setData(T data) {
        this.data = data;
    }

    @Override
    public boolean isSuccess() {
        return getCode() == CommonConstant.CODE_SUCCESS;
    }

}
