package com.bnorm.auto.weave.internal.chain;

import com.bnorm.auto.weave.AfterJoinPoint;
import com.bnorm.auto.weave.internal.Pointcut;

public abstract class AfterChain extends WrapChain {
    private final AfterJoinPoint afterJoinPoint;

    public AfterChain(Chain wrapped, Pointcut pointcut) {
        super(wrapped, pointcut);
        this.afterJoinPoint = new AfterJoinPoint(pointcut);
    }

    @Override
    public final Object call() throws Throwable {
        try {
            return super.call();
        } finally {
            after(afterJoinPoint);
        }
    }

    protected abstract void after(AfterJoinPoint afterJoinPoint);
}
