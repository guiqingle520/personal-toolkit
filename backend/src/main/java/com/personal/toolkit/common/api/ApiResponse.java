package com.personal.toolkit.common.api;

import java.time.OffsetDateTime;

/**
 * 统一封装接口成功响应体，向前端暴露成功标记、提示信息、数据载荷和响应时间。
 *
 * @param <T> 业务数据类型
 */
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final OffsetDateTime timestamp;

    private ApiResponse(boolean success, String message, T data, OffsetDateTime timestamp) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
    }

    /**
     * 创建带业务数据的成功响应。
     *
     * @param message 响应提示信息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 包含业务数据的统一响应体
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, OffsetDateTime.now());
    }

    /**
     * 创建不包含业务数据的成功响应。
     *
     * @param message 响应提示信息
     * @return 不包含 data 的统一响应体
     */
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null, OffsetDateTime.now());
    }

    /**
     * 返回当前响应是否成功。
     *
     * @return 成功标记
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 返回供前端展示的响应提示信息。
     *
     * @return 响应消息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 返回业务数据载荷。
     *
     * @return 响应数据
     */
    public T getData() {
        return data;
    }

    /**
     * 返回响应对象创建时间。
     *
     * @return 响应时间戳
     */
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }
}
