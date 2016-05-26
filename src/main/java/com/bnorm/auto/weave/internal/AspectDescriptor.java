package com.bnorm.auto.weave.internal;

import javax.lang.model.element.TypeElement;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class AspectDescriptor {

    static AspectDescriptor create(TypeElement element) {
        return new AutoValue_AspectDescriptor(element);
    }

    abstract TypeElement element();

    String name() {
        return element().getSimpleName().toString();
    }

    String fieldName() {
        return Character.toLowerCase(name().charAt(0)) + name().substring(1);
    }
}
