package com.bnorm.auto.weave.internal;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Pointcut {

    public static Pointcut create(String method) {
        return new AutoValue_Pointcut(method);
    }

    public abstract String method();
}
