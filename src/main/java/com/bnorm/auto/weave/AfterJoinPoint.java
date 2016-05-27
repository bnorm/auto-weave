package com.bnorm.auto.weave;

import com.bnorm.auto.weave.internal.JoinPoint;
import com.bnorm.auto.weave.internal.Pointcut;

public final class AfterJoinPoint extends JoinPoint {

    public AfterJoinPoint(Pointcut method) {
        super(method);
    }
}
