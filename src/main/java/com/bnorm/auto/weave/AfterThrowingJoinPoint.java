package com.bnorm.auto.weave;

import com.bnorm.auto.weave.internal.Pointcut;

public class AfterThrowingJoinPoint extends AfterJoinPoint {

    private final Throwable error;

    public AfterThrowingJoinPoint(Pointcut method, Throwable error) {
        super(method);
        this.error = error;
    }

    public Throwable error() {
        return error;
    }
}
