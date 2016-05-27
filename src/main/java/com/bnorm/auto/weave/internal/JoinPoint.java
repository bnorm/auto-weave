package com.bnorm.auto.weave.internal;

import java.util.List;

public class JoinPoint {

    private final Pointcut pointcut;

    protected JoinPoint(Pointcut pointcut) {
        this.pointcut = pointcut;
    }


    public Object target() {
        return pointcut.target();
    }

    public List<Object> args() {
        return pointcut.args();
    }


    public String method() {
        return pointcut.staticPointcut().method();
    }

    public Class<?> targetType() {
        return pointcut.staticPointcut().targetType();
    }

    public Class<?> returnType() {
        return pointcut.staticPointcut().returnType();
    }

    public List<Class<?>> argTypes() {
        return pointcut.staticPointcut().argTypes();
    }
}
