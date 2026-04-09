package com.personal.toolkit.common.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 统一封装接口错误响应体，补充状态码、请求路径和字段校验信息。
 */
public class ApiErrorResponse {

    private final boolean success;
    private final String message;
    private final int status;
    private final String path;
    private final Map<String, List<String>> validation;
    private final OffsetDateTime timestamp;

    private ApiErrorResponse(String message,
                             int status,
                             String path,
                             Map<String, List<String>> validation,
                             OffsetDateTime timestamp) {
        this.success = false;
        this.message = message;
        this.status = status;
        this.path = path;
        this.validation = validation;
        this.timestamp = timestamp;
    }

    /**
     * 创建不包含字段校验明细的错误响应。
     *
     * @param message 错误提示信息
     * @param status HTTP 状态码
     * @param path 当前请求路径
     * @return 统一错误响应体
     */
    public static ApiErrorResponse of(String message, int status, String path) {
        return new ApiErrorResponse(message, status, path, Map.of(), OffsetDateTime.now());
    }

    /**
     * 创建包含字段校验明细的错误响应。
     *
     * @param message 错误提示信息
     * @param status HTTP 状态码
     * @param path 当前请求路径
     * @param validation 字段级校验错误集合
     * @return 统一错误响应体
     */
    public static ApiErrorResponse of(String message,
                                      int status,
                                      String path,
                                      Map<String, List<String>> validation) {
        return new ApiErrorResponse(message, status, path, validation, OffsetDateTime.now());
    }

    /**
     * 返回当前响应是否成功。
     *
     * @return 固定为 false 的错误标记
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 返回错误提示信息。
     *
     * @return 错误消息
     */
    public String getMessage() {
        return message;
    }

    /**
     * 返回当前错误对应的 HTTP 状态码。
     *
     * @return HTTP 状态码
     */
    public int getStatus() {
        return status;
    }

    /**
     * 返回触发错误的请求路径。
     *
     * @return 请求路径
     */
    public String getPath() {
        return path;
    }

    /**
     * 返回字段级校验错误明细。
     *
     * @return 校验错误集合
     */
    public Map<String, List<String>> getValidation() {
        return validation;
    }

    /**
     * 返回错误响应生成时间。
     *
     * @return 响应时间戳
     */
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }
}
