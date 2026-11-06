package com.krishilink.marketservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String errorCode;

    public static <T> ApiResponse<T> ok(T data) { return ApiResponse.<T>builder().success(true).data(data).build(); }
    public static <T> ApiResponse<T> ok(T data, String message) { return ApiResponse.<T>builder().success(true).data(data).message(message).build(); }
    public static <T> ApiResponse<T> fail(String message, String errorCode) { return ApiResponse.<T>builder().success(false).message(message).errorCode(errorCode).build(); }
}
