package com.bnorm.auto.weave;

public class TraceAspect {

    @AutoAdvice(Trace.class)
    public void after(AfterJoinPoint point) {
        System.out.println("After method " + point.method());
    }

    @AutoAdvice({Trace.class, Validate.class})
    public Object around(AroundJoinPoint point) {
        return point.proceed();
    }
}
