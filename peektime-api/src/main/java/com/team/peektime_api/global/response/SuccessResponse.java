package com.team.peektime_api.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"code", "message", "result"})
public class SuccessResponse<T> {

    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;

    public static <T> SuccessResponse<T> ok(T result) {
        return new SuccessResponse<>(SuccessCode._OK.getCode(), SuccessCode._OK.getMessage(), result);
    }

    public static <T> SuccessResponse<T> of(SuccessCode code, T result) {
        return new SuccessResponse<>(code.getCode(), code.getMessage(), result);
    }

    public static SuccessResponse<Void> ok() {
        return new SuccessResponse<>(SuccessCode._OK.getCode(), SuccessCode._OK.getMessage(), null);
    }
}