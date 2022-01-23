package io.github.gfd.feignnative;


import java.io.Serializable;

/**
 * 统一返回对象
 */
public interface ApiResponse<T> extends Serializable {

    int getCode();

    void setCode(int code);

    String getMessage();

    void setMessage(String message);

    T getData();

    void setData(T data);

    boolean isSuccess();

}
