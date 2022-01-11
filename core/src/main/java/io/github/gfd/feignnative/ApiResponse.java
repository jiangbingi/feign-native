package io.github.gfd.feignnative;


import io.github.gfd.feignnative.constant.CommonConstant;

/**
 * 统一返回对象
 */
public interface ApiResponse<T> {

    int code();

    String message();

    T data();

    boolean isSuccess();

    ApiResponse<T> successData(T data);

    class Default<T> implements ApiResponse<T> {

        private int code;

        private String message;

        private T data;

        @Override
        public int code() {
            return code;
        }

        @Override
        public String message() {
            return message;
        }

        @Override
        public T data() {
            return data;
        }

        @Override
        public boolean isSuccess() {
            return code == CommonConstant.CODE_SUCCESS;
        }

        @Override
        public ApiResponse<T> successData(T data) {
            setCode(CommonConstant.CODE_SUCCESS);
            setData(data);
            return this;
        }

        public int getCode() {
            return code();
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message();
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public T getData() {
            return data();
        }

        public void setData(T data) {
            this.data = data;
        }
    }
}
