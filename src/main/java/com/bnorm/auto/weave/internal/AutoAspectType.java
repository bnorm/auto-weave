package com.bnorm.auto.weave.internal;

import javax.lang.model.element.TypeElement;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class AutoAspectType {

    public static AutoAspectType create(TypeElement type) {
        return new AutoValue_AutoAspectType(type);
    }

    public abstract TypeElement type();

    public String name() {
        return type().getSimpleName().toString();
    }

    public String fieldName() {
        return Character.toLowerCase(name().charAt(0)) + name().substring(1);
    }
}
