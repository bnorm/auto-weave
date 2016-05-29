package com.bnorm.auto.weave.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.TypeElement;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class WeaveDescriptor {

    static WeaveDescriptor create(TypeElement element, List<WeaveMethodDescriptor> methods) {
        Set<AspectDescriptor> aspects = new HashSet<>();
        for (WeaveMethodDescriptor method : methods) {
            aspects.addAll(method.aspects());
        }
        return new AutoValue_WeaveDescriptor(element, methods, aspects);
    }

    abstract TypeElement element();

    abstract List<WeaveMethodDescriptor> methods();

    abstract Set<AspectDescriptor> aspects();

    String name() {
        return element().getSimpleName().toString();
    }
}
