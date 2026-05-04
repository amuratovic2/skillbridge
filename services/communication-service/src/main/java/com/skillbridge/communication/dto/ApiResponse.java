package com.skillbridge.communication.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String error;
    private String message;
    private Object meta;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = true;
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> ok(T data, Object meta) {
        ApiResponse<T> r = ok(data);
        r.meta = meta;
        return r;
    }

    public static <T> ApiResponse<T> error(String message) {
        return error("error", message);
    }

    public static <T> ApiResponse<T> error(String error, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.success = false;
        r.error = error;
        r.message = message;
        return r;
    }
}
