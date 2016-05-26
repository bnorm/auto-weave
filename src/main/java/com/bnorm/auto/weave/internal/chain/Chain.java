package com.bnorm.auto.weave.internal.chain;

public abstract class Chain {

    protected Chain() {
    }

    public abstract Object call() throws Throwable;
}
