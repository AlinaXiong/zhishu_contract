package com.hero.middleware.aspect;

import com.hero.middleware.context.ApiLogTableContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SkipApiLogTableAspect {

    @Around("@within(com.hero.middleware.annotation.SkipApiLogTable)"
            + " || @annotation(com.hero.middleware.annotation.SkipApiLogTable)")
    public Object suppressTableLog(ProceedingJoinPoint point) throws Throwable {
        ApiLogTableContext.enterSuppressed();
        try {
            return point.proceed();
        } finally {
            ApiLogTableContext.exitSuppressed();
        }
    }
}
