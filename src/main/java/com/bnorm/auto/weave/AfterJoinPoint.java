package com.bnorm.auto.weave;

import com.bnorm.auto.weave.internal.JoinPoint;
import com.bnorm.auto.weave.internal.Pointcut;

public class AfterJoinPoint extends JoinPoint {

    public AfterJoinPoint(Pointcut method) {
        super(method);
    }
}
