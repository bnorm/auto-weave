package com.bnorm.auto.weave.internal.chain;

import com.bnorm.auto.weave.AfterReturningJoinPoint;
import com.bnorm.auto.weave.internal.Pointcut;

public abstract class AfterReturningChain extends WrapChain {

    public AfterReturningChain(Chain wrapped, Pointcut pointcut) {
        super(wrapped, pointcut);
    }

    @Override
    public final Object call() {
        Object result = super.call();
        AfterReturningJoinPoint afterThrowingJoinPoint = new AfterReturningJoinPoint(pointcut, result);
        afterReturning(afterThrowingJoinPoint);
        return result;
    }

    protected abstract void afterReturning(AfterReturningJoinPoint afterJoinPoint);
}
