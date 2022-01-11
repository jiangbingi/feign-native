package io.github.telxs.feignnative.demo.api.model;

import io.github.gfd.feignnative.ApiResponse;

public class FeignResponse<T> implements ApiResponse<T> {

    public static final int CODE_SUCCESS = 200;

    private int code;

    private String msg;

    private T data;

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return msg;
    }

    @Override
    public T data() {
        return data;
    }

    @Override
    public boolean isSuccess() {
        return code() == CODE_SUCCESS;
    }

    @Override
    public ApiResponse<T> successData(T data) {
        setCode(CODE_SUCCESS);
        setMsg("成功");
        setData(data);
        return this;
    }

    public static <T> FeignResponse<T> success(T data){
        FeignResponse<T> response = new FeignResponse<>();
        response.successData(data);
        return response;
    }

    public int getCode() {
        return code();
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return message();
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data();
    }

    public void setData(T data) {
        this.data = data;
    }
}
