package com.team.peektime_admin.global.exception;

import com.team.peektime_admin.global.response.ErrorCode;
import com.team.peektime_admin.global.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
@Order(2)
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, Model model, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.BAD_REQUEST.getMessage());
        return handleBadRequest(message, model, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgumentException(IllegalArgumentException e, Model model, HttpServletRequest request) {
        return handleBadRequest(e.getMessage(), model, request);
    }

    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException e, Model model, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return ResponseEntity
                    .status(e.getErrorCode().getHttpStatus())
                    .body(ErrorResponse.of(e.getErrorCode()));
        }
        model.addAttribute("errorCode", e.getErrorCode().getCode());
        model.addAttribute("errorMessage", e.getErrorCode().getMessage());
        model.addAttribute("status", e.getErrorCode().getHttpStatus().value());
        return "error/error";
    }

    private Object handleBadRequest(String message, Model model, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return ResponseEntity
                    .status(ErrorCode.BAD_REQUEST.getHttpStatus())
                    .body(ErrorResponse.of(ErrorCode.BAD_REQUEST.getCode(), message));
        }
        model.addAttribute("errorCode", ErrorCode.BAD_REQUEST.getCode());
        model.addAttribute("errorMessage", message);
        model.addAttribute("status", ErrorCode.BAD_REQUEST.getHttpStatus().value());
        return "error/error";
    }

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e, Model model, HttpServletRequest request) throws Exception {
        // H2 콘솔 요청은 예외 처리하지 않음
        if (isH2ConsoleRequest(request)) {
            throw e;
        }
        log.error("Unhandled exception: ", e);
        if (isApiRequest(request)) {
            return ResponseEntity
                    .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                    .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), e.getMessage()));
        }
        model.addAttribute("errorCode", ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        model.addAttribute("errorMessage", e.getMessage());
        model.addAttribute("status", ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus().value());
        return "error/error";
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/");
    }

    private boolean isH2ConsoleRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/h2-console");
    }
}
