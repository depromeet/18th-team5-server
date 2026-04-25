package com.team.peektime_admin.global.exception;

import com.team.peektime_admin.global.response.ErrorCode;
import com.team.peektime_admin.global.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception e, Model model, HttpServletRequest request) {
        log.error("Unhandled exception: ", e);
        if (isApiRequest(request)) {
            return ResponseEntity
                    .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                    .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage()));
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
}
