package com.bnorm.auto.weave;

import com.bnorm.auto.weave.internal.Pointcut;

public class AfterReturningJoinPoint extends AfterJoinPoint {

    private final Object result;

    public AfterReturningJoinPoint(Pointcut method, Object result) {
        super(method);
        this.result = result;
    }

    public Object result() {
        return result;
    }
}
