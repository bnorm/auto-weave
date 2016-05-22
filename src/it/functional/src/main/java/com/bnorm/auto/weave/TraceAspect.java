package com.bnorm.auto.weave;

public class TraceAspect {

    @AutoAspect(Trace.class)
    public Object around(AroundJoinPoint point) throws Throwable {
        System.out.println("Starting method " + point.method());
        Object result = point.proceed();
        System.out.println("Completed method " + point.method() + " with a result of " + result);
        return result;
    }
}
