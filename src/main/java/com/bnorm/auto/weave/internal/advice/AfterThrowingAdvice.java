package com.bnorm.auto.weave.internal.advice;

import com.bnorm.auto.weave.AfterThrowingJoinPoint;

public abstract class AfterThrowingAdvice implements Advice {

    @Override
    public final Object call(Chain chain) {
        try {
            return chain.proceed();
        } catch (MethodException e) {
            afterThrowing(chain);
            throw e;
        }
    }

    public abstract void afterThrowing(AfterThrowingJoinPoint afterThrowingJoinPoint);
}
