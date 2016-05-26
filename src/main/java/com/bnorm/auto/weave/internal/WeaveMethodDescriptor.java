package com.bnorm.auto.weave.internal;

import javax.lang.model.element.ExecutableElement;

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@AutoValue
abstract class WeaveMethodDescriptor {

    static WeaveMethodDescriptor create(ExecutableElement element, ImmutableList<AdviceDescriptor> advice) {
        return new AutoValue_WeaveMethodDescriptor(element, advice);
    }

    abstract ExecutableElement element();

    abstract ImmutableList<AdviceDescriptor> advice();

    ImmutableSet<AspectDescriptor> aspects() {
        return FluentIterable.from(advice()).transform(new Function<AdviceDescriptor, AspectDescriptor>() {
            @Override
            public AspectDescriptor apply(AdviceDescriptor input) {
                return input.aspect();
            }
        }).toSet();
    }

    String name() {
        return element().getSimpleName().toString();
    }
}
