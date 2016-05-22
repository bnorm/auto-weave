package com.bnorm.auto.weave.internal.chain;

import com.bnorm.auto.weave.BeforeJoinPoint;
import com.bnorm.auto.weave.internal.Pointcut;

public abstract class BeforeChain extends Chain {
    private final BeforeJoinPoint beforeJoinPoint;

    public BeforeChain(Chain wrapped, Pointcut pointcut) {
        super(wrapped, pointcut);
        this.beforeJoinPoint = new BeforeJoinPoint(pointcut);
    }

    @Override
    public final Object call() throws Throwable {
        before(beforeJoinPoint);
        return super.call();
    }

    protected abstract void before(BeforeJoinPoint beforeJoinPoint);
}
