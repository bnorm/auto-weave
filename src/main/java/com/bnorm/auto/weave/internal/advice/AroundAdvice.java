package com.bnorm.auto.weave.internal.advice;

import com.bnorm.auto.weave.AroundJoinPoint;

public abstract class AroundAdvice implements Advice {

    @Override
    public final Object call(Chain chain) {
        return around(chain);
    }

    public abstract Object around(AroundJoinPoint aroundJoinPoint);
}
