package com.bnorm.auto.weave.internal.chain;

public abstract class VoidMethodChain extends Chain {

    protected abstract void method() throws Throwable;

    @Override
    public final Object call() {
        try {
            method();
            return null;
        } catch (Throwable error) {
            throw new MethodException(error);
        }
    }
}
