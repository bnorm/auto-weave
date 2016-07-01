package com.bnorm.auto.weave.internal.advice;

import com.bnorm.auto.weave.AfterReturningJoinPoint;

public abstract class AfterReturningAdvice implements Advice {

    @Override
    public final Object call(Chain chain) {
        Object result = chain.proceed();
        afterReturning(chain);
        return result;
    }

    protected abstract void afterReturning(AfterReturningJoinPoint afterReturningJoinPoint);
}
