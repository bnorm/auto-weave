package com.bnorm.auto.weave.internal;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

@AutoValue
abstract class AdviceDescriptor {

    static AdviceDescriptor create(AspectDescriptor aspect, ExecutableElement element, CrosscutEnum crosscut,
                                   ImmutableSet<TypeMirror> targets) {
        return new AutoValue_AdviceDescriptor(aspect, element, crosscut, targets);
    }

    abstract AspectDescriptor aspect();

    abstract ExecutableElement element();

    abstract CrosscutEnum crosscut();

    abstract ImmutableSet<TypeMirror> targets();

    String name() {
        return element().getSimpleName().toString();
    }
}
