package com.bnorm.auto.weave.internal.advice;

import com.bnorm.auto.weave.BeforeJoinPoint;

public abstract class BeforeAdvice implements Advice {

    @Override
    public final Object call(Chain chain) {
        before(chain);
        return chain.proceed();
    }

    protected abstract void before(BeforeJoinPoint beforeJoinPoint);
}
