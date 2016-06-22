package com.bnorm.auto.weave;

public interface AroundJoinPoint extends JoinPoint {

    Object proceed();
}
