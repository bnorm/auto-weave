package com.bnorm.auto.weave.internal.chain;

public abstract class VoidMethodChain extends Chain {

    public VoidMethodChain() {
        super(null, null);
    }

    public abstract void method() throws Throwable;

    @Override
    public Object call() throws Throwable {
        method();
        return null;
    }
}
