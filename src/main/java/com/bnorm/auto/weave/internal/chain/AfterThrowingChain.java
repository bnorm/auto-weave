package com.bnorm.auto.weave.internal.chain;

import com.bnorm.auto.weave.AfterThrowingJoinPoint;
import com.bnorm.auto.weave.internal.Pointcut;

public abstract class AfterThrowingChain extends WrapChain {

    public AfterThrowingChain(Chain wrapped, Pointcut pointcut) {
        super(wrapped, pointcut);
    }

    @Override
    public final Object call() {
        try {
            return super.call();
        } catch (MethodException error) {
            AfterThrowingJoinPoint afterThrowingJoinPoint = new AfterThrowingJoinPoint(pointcut, error.getCause());
            afterThrowing(afterThrowingJoinPoint);
            throw error;
        }
    }

    protected abstract void afterThrowing(AfterThrowingJoinPoint afterJoinPoint);
}
