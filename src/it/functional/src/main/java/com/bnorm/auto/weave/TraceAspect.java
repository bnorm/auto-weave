package com.bnorm.auto.weave;

public class TraceAspect {

    @AutoAdvice(Trace.class)
    public void around(AfterJoinPoint point) {
        System.out.println("After method " + point.method());
    }
}
