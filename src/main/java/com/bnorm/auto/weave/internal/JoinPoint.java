package com.bnorm.auto.weave.internal;

public class JoinPoint {

    private final Pointcut pointcut;

    protected JoinPoint(Pointcut pointcut) {
        this.pointcut = pointcut;
    }

    public String method() {
        return pointcut.method();
    }
}
