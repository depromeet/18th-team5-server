package com.team.peektime_api.global.logging;

import com.team.peektime_api.global.auth.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
public class MethodLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(MethodLoggingAspect.class);
    private static final Set<String> recentLogCache = new HashSet<>();
    private static final int CACHE_SIZE_LIMIT = 1000;

    @Around("@within(org.springframework.stereotype.Controller) || " +
            "@within(org.springframework.web.bind.annotation.RestController)")
    public Object logControllerMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecutionWithType(joinPoint, "Controller");
    }

    @Around("@within(org.springframework.stereotype.Service)")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecutionWithType(joinPoint, "Service");
    }

    private Object logMethodExecutionWithType(ProceedingJoinPoint joinPoint, String type) throws Throwable {
        Logger targetLogger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        String requestInfo = "";
        String userInfo = "";
        if ("Controller".equals(type)) {
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                        .getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    requestInfo = String.format(" [%s %s]", request.getMethod(), request.getRequestURI());
                }
            } catch (Exception e) {
                // 예외 무시
            }

            userInfo = getUserInfo();
        }

        String arguments = Arrays.stream(joinPoint.getArgs())
                .map(arg -> arg == null ? "null" : summarizeObject(arg))
                .collect(Collectors.joining(", "));

        String logKey = className + "." + methodName + "(" + arguments + ")";

        if (recentLogCache.size() > CACHE_SIZE_LIMIT) {
            recentLogCache.clear();
        }

        boolean isDuplicate = !recentLogCache.add(logKey);

        if (!isDuplicate || "Controller".equals(type)) {
            targetLogger.info("{} 시작: {}.{}{}{} - 파라미터: [{}]",
                    type, className, methodName, requestInfo, userInfo, arguments);
        }

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();

            String resultSummary = result == null ? "null" : summarizeObject(result);

            if (!isDuplicate || "Controller".equals(type)) {
                targetLogger.info("{} 완료: {}.{}{}{} - 실행시간: {}ms - 결과: {}",
                        type, className, methodName, requestInfo, userInfo,
                        stopWatch.getTotalTimeMillis(), resultSummary);
            }

            return result;
        } catch (Exception e) {
            stopWatch.stop();

            targetLogger.error("{} 예외: {}.{}{}{} - 실행시간: {}ms - 예외: {}",
                    type, className, methodName, requestInfo, userInfo,
                    stopWatch.getTotalTimeMillis(), e.getMessage(), e);

            throw e;
        }
    }

    private String getUserInfo() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
                return String.format(" [userId=%d, uuid=%s]", principal.getUserId(), principal.getDeviceUuid());
            }
        } catch (Exception e) {
            // 예외 무시
        }
        return "";
    }

    private String summarizeObject(Object obj) {
        if (obj == null) {
            return "null";
        }

        try {
            if (obj instanceof HttpServletRequest req) {
                return String.format("HttpServletRequest[%s %s]", req.getMethod(), req.getRequestURI());
            }

            String str = obj.toString();

            if (str.length() > 300) {
                return str.substring(0, 300) + "...";
            }

            return str;
        } catch (Exception e) {
            return obj.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(obj));
        }
    }
}