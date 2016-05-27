package com.bnorm.auto.weave;

import com.bnorm.auto.weave.internal.JoinPoint;
import com.bnorm.auto.weave.internal.Pointcut;

public final class BeforeJoinPoint extends JoinPoint {

    public BeforeJoinPoint(Pointcut method) {
        super(method);
    }
}
