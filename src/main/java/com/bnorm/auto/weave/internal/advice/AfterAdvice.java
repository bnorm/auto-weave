package com.bnorm.auto.weave.internal.advice;

import com.bnorm.auto.weave.AfterJoinPoint;

public abstract class AfterAdvice implements Advice {

    @Override
    public final Object call(Chain chain) {
        try {
            return chain.proceed();
        } finally {
            after(chain);
        }
    }

    public abstract void after(AfterJoinPoint afterJoinPoint);
}
