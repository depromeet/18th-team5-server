package com.team.peektime_admin.global.exception;

import com.team.peektime_admin.global.response.ErrorCode;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException e, Model model) {
        model.addAttribute("errorCode", e.getErrorCode().getCode());
        model.addAttribute("errorMessage", e.getErrorCode().getMessage());
        model.addAttribute("status", e.getErrorCode().getHttpStatus().value());
        return "error/error";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        model.addAttribute("errorCode", ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        model.addAttribute("errorMessage", ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
        model.addAttribute("status", ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus().value());
        return "error/error";
    }
}
