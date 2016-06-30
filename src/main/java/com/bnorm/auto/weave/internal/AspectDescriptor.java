package com.bnorm.auto.weave.internal;

import javax.lang.model.element.TypeElement;

import com.bnorm.auto.weave.AutoAspect;
import com.google.auto.value.AutoValue;

@AutoValue
abstract class AspectDescriptor {

    static AspectDescriptor create(TypeElement element, AutoAspect.AspectInit aspectInit) {
        return new AutoValue_AspectDescriptor(element, aspectInit);
    }

    abstract TypeElement element();

    abstract AutoAspect.AspectInit initialization();

    String name() {
        return element().getSimpleName().toString();
    }

    String fieldName() {
        return Character.toLowerCase(name().charAt(0)) + name().substring(1);
    }
}
