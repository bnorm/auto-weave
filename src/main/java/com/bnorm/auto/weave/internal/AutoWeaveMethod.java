package com.bnorm.auto.weave.internal;

import java.util.List;
import javax.lang.model.element.ExecutableElement;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class AutoWeaveMethod {

    public static AutoWeaveMethod create(ExecutableElement method, List<AutoAspectMethod> calls) {
        return new AutoValue_AutoWeaveMethod(method, calls);
    }

    public abstract ExecutableElement method();

    public abstract List<AutoAspectMethod> aspectMethods();
}
