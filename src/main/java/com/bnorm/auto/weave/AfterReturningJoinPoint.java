package com.bnorm.auto.weave;

import com.bnorm.auto.weave.internal.JoinPoint;
import com.bnorm.auto.weave.internal.Pointcut;

public final class AfterReturningJoinPoint extends JoinPoint {

    private final Object result;

    public AfterReturningJoinPoint(Pointcut method, Object result) {
        super(method);
        this.result = result;
    }

    public Object result() {
        return result;
    }
}
