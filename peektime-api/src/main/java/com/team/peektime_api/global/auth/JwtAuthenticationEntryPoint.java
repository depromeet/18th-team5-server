package com.team.peektime_api.global.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.peektime_api.global.response.ErrorCode;
import com.team.peektime_api.global.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorCode errorCode = resolveErrorCode(request);
        response.getWriter().write(objectMapper.writeValueAsString(ErrorResponse.of(errorCode)));
    }

    private ErrorCode resolveErrorCode(HttpServletRequest request) {
        Object attribute = request.getAttribute("jwt.error");
        if ("expired".equals(attribute)) {
            return ErrorCode.EXPIRED_TOKEN;
        }
        return ErrorCode.INVALID_TOKEN;
    }
}
