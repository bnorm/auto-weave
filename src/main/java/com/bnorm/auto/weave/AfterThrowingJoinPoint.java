package com.bnorm.auto.weave;

import com.bnorm.auto.weave.internal.JoinPoint;
import com.bnorm.auto.weave.internal.Pointcut;

public final class AfterThrowingJoinPoint extends JoinPoint {

    private final Throwable error;

    public AfterThrowingJoinPoint(Pointcut method, Throwable error) {
        super(method);
        this.error = error;
    }

    public Throwable error() {
        return error;
    }
}
