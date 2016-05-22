package com.bnorm.auto.weave.internal;

import javax.lang.model.element.ExecutableElement;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class AutoAspectMethod {

    public static AutoAspectMethod create(ExecutableElement method, CrosscutEnum crosscut, AutoAspectType type) {
        return new AutoValue_AutoAspectMethod(method, crosscut, type);
    }

    public abstract ExecutableElement method();

    public abstract CrosscutEnum crosscut();

    public String name() {
        return method().getSimpleName().toString();
    }

    public abstract AutoAspectType aspect();
}
