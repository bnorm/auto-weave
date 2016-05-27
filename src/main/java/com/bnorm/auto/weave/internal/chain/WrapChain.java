package com.bnorm.auto.weave.internal.chain;

import com.bnorm.auto.weave.internal.Pointcut;

abstract class WrapChain extends Chain {

    private final Chain wrapped;
    protected final Pointcut pointcut;

    WrapChain(Chain wrapped, Pointcut pointcut) {
        this.wrapped = wrapped;
        this.pointcut = pointcut;
    }

    public Object call() {
        return wrapped.call();
    }
}
