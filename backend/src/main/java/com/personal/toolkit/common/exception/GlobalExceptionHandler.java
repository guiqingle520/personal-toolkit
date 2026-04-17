package com.personal.toolkit.common.exception;

import com.personal.toolkit.common.api.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统一处理控制层抛出的异常，并转换为前端可稳定消费的错误响应结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理请求体字段校验失败的异常，并返回字段级错误明细。
     *
     * @param ex 字段校验异常
     * @param request 当前请求对象
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                         HttpServletRequest request) {
        Map<String, List<String>> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
                ));

        ApiErrorResponse response = ApiErrorResponse.of(
                "Validation failed",
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                validationErrors
        );
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理参数约束校验失败的异常，并聚合为字段级错误列表。
     *
     * @param ex 参数约束异常
     * @param request 当前请求对象
     * @return 统一错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                      HttpServletRequest request) {
        Map<String, List<String>> validationErrors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.groupingBy(
                        violation -> violation.getPropertyPath().toString(),
                        Collectors.mapping(violation -> violation.getMessage(), Collectors.toList())
                ));

        ApiErrorResponse response = ApiErrorResponse.of(
                "Validation failed",
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                validationErrors
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex,
                                                               HttpServletRequest request) {
        ApiErrorResponse response = ApiErrorResponse.of(
                ex.getCode(),
                ex.getMessage(),
                ex.getStatusCode().value(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    /**
     * 处理业务流程中主动抛出的 ResponseStatusException，保留原始状态码和原因。
     *
     * @param ex 业务状态异常
     * @param request 当前请求对象
     * @return 统一错误响应
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex,
                                                                 HttpServletRequest request) {
        String message = ex.getReason() != null ? ex.getReason() : "Request failed";
        ApiErrorResponse response = ApiErrorResponse.of(
                message,
                ex.getStatusCode().value(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    /**
     * 处理未被显式捕获的异常，避免堆栈信息直接暴露给前端。
     *
     * @param ex 未知异常
     * @param request 当前请求对象
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex,
                                                                   HttpServletRequest request) {
        log.error("Unhandled exception while processing request {}", request.getRequestURI(), ex);
        ApiErrorResponse response = ApiErrorResponse.of(
                "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
