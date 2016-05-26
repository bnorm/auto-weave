package com.bnorm.auto.weave.internal.chain;

import com.bnorm.auto.weave.AroundJoinPoint;
import com.bnorm.auto.weave.internal.Pointcut;

public abstract class AroundChain extends WrapChain {

    private final AroundJoinPoint aroundJoinPoint;

    public AroundChain(Chain wrapped, Pointcut pointcut) {
        super(wrapped, pointcut);
        aroundJoinPoint = new AroundJoinPoint(pointcut, wrapped);
    }

    public final Object call() throws Throwable {
        return around(aroundJoinPoint);
    }

    protected abstract Object around(AroundJoinPoint around) throws Throwable;
}
