package com.bnorm.auto.weave.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class WeaveMethodDescriptor {

    static WeaveMethodDescriptor create(ExecutableElement element, List<AdviceDescriptor> advice) {
        Set<AspectDescriptor> aspects = new HashSet<>();
        for (AdviceDescriptor descriptor : advice) {
            aspects.add(descriptor.aspect());
        }
        return new AutoValue_WeaveMethodDescriptor(element, advice, aspects);
    }

    abstract ExecutableElement element();

    abstract List<AdviceDescriptor> advice();

    abstract Set<AspectDescriptor> aspects();

    String name() {
        return element().getSimpleName().toString();
    }
}
