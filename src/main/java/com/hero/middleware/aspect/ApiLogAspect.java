package com.hero.middleware.aspect;

import com.alibaba.fastjson.JSON;
import com.hero.middleware.annotation.ApiLog;
import com.hero.middleware.entity.ApiLogRecord;
import com.hero.middleware.mapper.ApiLogRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Aspect
@Component
public class ApiLogAspect {

    @Autowired
    private ApiLogRecordMapper apiLogRecordMapper;

    @Pointcut("@annotation(com.hero.middleware.annotation.ApiLog)")
    public void apiLogPointcut() {
    }

    @Around("apiLogPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        ApiLog apiLog = method.getAnnotation(ApiLog.class);
        
        String apiName = apiLog.value();
        if (apiName == null || apiName.isEmpty()) {
            apiName = method.getName();
        }
        String apiDescription = apiLog.description();
        
        HttpServletRequest request = getRequest();
        
        ApiLogRecord logRecord = new ApiLogRecord();
        logRecord.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        logRecord.setApiName(apiName);
        logRecord.setApiDescription(apiDescription);
        
        if (request != null) {
            logRecord.setRequestMethod(request.getMethod());
            logRecord.setRequestUrl(request.getRequestURI());
            logRecord.setRequestIp(getIpAddress(request));
            logRecord.setUserAgent(request.getHeader("User-Agent"));
        }
        
        Object[] args = point.getArgs();
        Object[] filteredArgs = filterArgs(args);
        try {
            logRecord.setRequestParams(JSON.toJSONString(filteredArgs));
        } catch (Exception e) {
            logRecord.setRequestParams("参数序列化失败: " + e.getMessage());
        }
        
        Object result = null;
        Throwable exception = null;
        
        try {
            result = point.proceed();
            logRecord.setStatus(1);
            logRecord.setHttpStatus(200);
            try {
                logRecord.setResponseParams(JSON.toJSONString(result));
            } catch (Exception e) {
                logRecord.setResponseParams("响应序列化失败: " + e.getMessage());
            }
        } catch (Throwable e) {
            exception = e;
            logRecord.setStatus(0);
            logRecord.setErrorMessage(e.getMessage());
            log.info("API接口执行异常: {}", e.getMessage());
        }
        
        long endTime = System.currentTimeMillis();
        logRecord.setExecuteTime(endTime - startTime);
        logRecord.setCreateTime(LocalDateTime.now());
        
        try {
            apiLogRecordMapper.insert(logRecord);
        } catch (Exception e) {
            log.error("保存API日志失败: {}", e.getMessage(), e);
        }
        
        if (exception != null) {
            throw exception;
        }
        
        return result;
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private Object[] filterArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return args;
        }
        List<Object> filteredList = new ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest 
                || arg instanceof HttpServletResponse 
                || arg instanceof MultipartFile) {
                continue;
            }
            filteredList.add(arg);
        }
        return filteredList.toArray();
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

}
