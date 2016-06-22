package com.bnorm.auto.weave;

public interface AfterThrowingJoinPoint extends AfterJoinPoint {

    Throwable error();
}
