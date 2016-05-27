package com.bnorm.auto.weave.internal;

import java.util.List;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Pointcut {

    public static Pointcut create(Object target, List<Object> args, StaticPointcut staticPointcut) {
        return new AutoValue_Pointcut(target, args, staticPointcut);
    }

    public abstract Object target();

    public abstract List<Object> args();

    public abstract StaticPointcut staticPointcut();
}
