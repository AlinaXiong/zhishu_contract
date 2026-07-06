package com.hero.middleware.aspect;

import com.hero.middleware.component.ZhishuEventLogRecorder;
import com.hero.middleware.context.ApiLogTaskContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ZhishuEventLogAspect {

    private final ZhishuEventLogRecorder recorder;

    public ZhishuEventLogAspect(ZhishuEventLogRecorder recorder) {
        this.recorder = recorder;
    }

    @Around("@annotation(com.hero.middleware.annotation.ZhishuEventLog)")
    public Object recordEvent(ProceedingJoinPoint point) throws Throwable {
        ZhishuEventLogRecorder.PreparedEvent event = recorder.capture(point.getArgs());
        if (event == null) {
            return point.proceed();
        }

        boolean success = false;
        ApiLogTaskContext.enter(event.getTaskId());
        try {
            Object result = point.proceed();
            success = true;
            return result;
        } finally {
            try {
                recorder.record(event, success);
            } finally {
                ApiLogTaskContext.exit();
            }
        }
    }
}
