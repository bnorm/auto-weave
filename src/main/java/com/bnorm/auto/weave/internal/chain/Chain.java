package com.bnorm.auto.weave.internal.chain;

import com.bnorm.auto.weave.internal.Pointcut;

public class Chain {

    private final Chain wrapped;
    private final Pointcut pointcut;

    public Chain(Chain wrapped, Pointcut pointcut) {
        this.wrapped = wrapped;
        this.pointcut = pointcut;
    }

    public Object call() throws Throwable {
        return wrapped.call();
    }

    protected Object around() throws Throwable {
        before();
        Object result;
        try {
            result = wrapped.call();
            afterReturning(result);
        } catch (Throwable e) {
            afterThrowing(e);
            throw e;
        } finally {
            after();
        }
        return result;
    }

    protected void before() {
    }

    protected void afterReturning(Object result) {
    }

    protected void afterThrowing(Throwable e) {
    }

    protected void after() {
    }
}
