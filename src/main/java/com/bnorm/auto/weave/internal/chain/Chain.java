package com.bnorm.auto.weave.internal.chain;

import com.bnorm.auto.weave.internal.Pointcut;

public abstract class Chain {

    private final Chain wrapped;
    protected final Pointcut pointcut;

    protected Chain(Chain wrapped, Pointcut pointcut) {
        this.wrapped = wrapped;
        this.pointcut = pointcut;
    }

    public Object call() throws Throwable {
        return wrapped.call();
    }
}
