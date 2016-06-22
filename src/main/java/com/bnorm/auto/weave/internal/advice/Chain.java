package com.bnorm.auto.weave.internal.advice;

import java.util.List;

import com.bnorm.auto.weave.AfterJoinPoint;
import com.bnorm.auto.weave.AfterReturningJoinPoint;
import com.bnorm.auto.weave.AfterThrowingJoinPoint;
import com.bnorm.auto.weave.AroundJoinPoint;
import com.bnorm.auto.weave.BeforeJoinPoint;
import com.bnorm.auto.weave.internal.StaticPointcut;

public abstract class Chain
        implements AfterJoinPoint, AfterReturningJoinPoint, AfterThrowingJoinPoint, AroundJoinPoint, BeforeJoinPoint {

    private final Advice[] advice;
    private final Object target;
    private final StaticPointcut staticPointcut;
    private final List<?> args;

    private int index;
    private Object result;
    private Throwable error;

    protected Chain(Advice[] advice, Object target, StaticPointcut staticPointcut, List<?> args) {
        this.advice = advice;
        this.target = target;
        this.staticPointcut = staticPointcut;
        this.args = args;

        this.index = 0;
        this.result = null;
        this.error = null;
    }

    public Object proceed() {
        if (index < advice.length) {
            return advice[index++].call(this);
        } else {
            try {
                this.result = call();
                return result;
            } catch (Throwable error) {
                this.error = error;
                throw new RuntimeException(error);
            }
        }
    }

    protected abstract Object call() throws Throwable;

    @Override
    public Object result() {
        return result;
    }

    @Override
    public Throwable error() {
        return error;
    }

    @Override
    public Object target() {
        return target;
    }

    @Override
    public List<?> args() {
        return args;
    }

    @Override
    public String method() {
        return staticPointcut.method();
    }

    @Override
    public Class<?> targetType() {
        return staticPointcut.targetType();
    }

    @Override
    public Class<?> returnType() {
        return staticPointcut.returnType();
    }

    @Override
    public List<Class<?>> argTypes() {
        return staticPointcut.argTypes();
    }
}
