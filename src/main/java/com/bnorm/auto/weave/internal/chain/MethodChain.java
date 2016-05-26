package com.bnorm.auto.weave.internal.chain;

public abstract class MethodChain extends Chain {

    public abstract Object method() throws Throwable;

    @Override
    public Object call() throws Throwable {
        return method();
    }
}
