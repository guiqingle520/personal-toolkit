package com.personal.toolkit.common.exception;

import org.springframework.http.HttpStatusCode;

/**
 * 表示需要保留业务错误码的接口异常。
 */
public class ApiException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final String code;

    public ApiException(HttpStatusCode statusCode, String code, String message) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public String getCode() {
        return code;
    }
}
