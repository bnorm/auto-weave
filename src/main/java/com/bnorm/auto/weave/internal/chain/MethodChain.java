package com.bnorm.auto.weave.internal.chain;

public abstract class MethodChain extends Chain {

    protected abstract Object method() throws Throwable;

    @Override
    public final Object call() {
        try {
            return method();
        } catch (Throwable error) {
            throw new MethodException(error);
        }
    }
}
