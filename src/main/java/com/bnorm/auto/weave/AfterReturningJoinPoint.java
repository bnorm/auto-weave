package com.bnorm.auto.weave;

public interface AfterReturningJoinPoint extends AfterJoinPoint {

    Object result();
}
