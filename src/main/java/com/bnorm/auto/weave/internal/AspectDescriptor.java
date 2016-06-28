package com.bnorm.auto.weave.internal;

import javax.lang.model.element.TypeElement;

import com.bnorm.auto.weave.AutoAspect;
import com.google.auto.value.AutoValue;

@AutoValue
abstract class AspectDescriptor {

    static AspectDescriptor create(TypeElement element, AutoAspect.Initialization initialization) {
        return new AutoValue_AspectDescriptor(element, initialization);
    }

    abstract TypeElement element();

    abstract AutoAspect.Initialization initialization();

    String name() {
        return element().getSimpleName().toString();
    }

    String fieldName() {
        return Character.toLowerCase(name().charAt(0)) + name().substring(1);
    }
}
