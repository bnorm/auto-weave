package com.bnorm.auto.weave.internal.chain;

public abstract class VoidMethodChain extends Chain {

    public abstract void method() throws Throwable;

    @Override
    public Object call() throws Throwable {
        method();
        return null;
    }
}
