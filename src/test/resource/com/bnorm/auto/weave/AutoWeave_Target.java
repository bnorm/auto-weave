package com.bnorm.auto.weave;

import com.bnorm.auto.weave.internal.chain.Chain;
import com.bnorm.auto.weave.internal.Pointcut;
import com.bnorm.auto.weave.internal.chain.VoidMethodChain;

final class AutoWeave_Target extends Target {

    private static final Pointcut methodPointcut = Pointcut.create("method");

    private final TraceAspect method_TraceAspect = new TraceAspect();
    private final TraceBeforeAspect method_TraceBeforeAspect = new TraceBeforeAspect();

    @Override
    public void method() {
        Chain chain;

        chain = new VoidMethodChain() {
            @Override
            public void method() throws Throwable {
                AutoWeave_Target.super.method();
            }
        };

        chain = new Chain(chain, methodPointcut) {
            @Override
            public Object around() throws Throwable {
                return method_TraceAspect.around(createAroundJoinPoint());
            }
        };

        chain = new Chain(chain, methodPointcut) {
            @Override
            public void before() {
                method_TraceBeforeAspect.before(createBeforeJoinPoint());
            }
        };

        try {
            chain.call();
        } catch (Throwable e) {
            if (e instanceof Error) {
                throw (Error) e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new AssertionError("Please contact the library developer", e);
            }
        }
    }
}
