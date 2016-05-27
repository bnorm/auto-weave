package com.bnorm.auto.weave;

import com.bnorm.auto.weave.internal.JoinPoint;
import com.bnorm.auto.weave.internal.Pointcut;
import com.bnorm.auto.weave.internal.chain.Chain;

public class AroundJoinPoint extends JoinPoint {

    private final Chain callable;

    public AroundJoinPoint(Pointcut method, Chain callable) {
        super(method);
        this.callable = callable;
    }

    public Object proceed() {
        return callable.call();
    }
}
