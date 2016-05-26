package com.bnorm.auto.weave.internal;

import javax.lang.model.element.TypeElement;

import com.google.auto.value.AutoValue;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@AutoValue
abstract class WeaveDescriptor {

    static WeaveDescriptor create(TypeElement element, ImmutableList<WeaveMethodDescriptor> methods) {
        return new AutoValue_WeaveDescriptor(element, methods);
    }

    abstract TypeElement element();

    abstract ImmutableList<WeaveMethodDescriptor> methods();

    ImmutableSet<AspectDescriptor> aspects() {
        return FluentIterable.from(methods())
                             .transformAndConcat(new Function<WeaveMethodDescriptor, ImmutableSet<AspectDescriptor>>() {
                                 @Override
                                 public ImmutableSet<AspectDescriptor> apply(WeaveMethodDescriptor input) {
                                     return input.aspects();
                                 }
                             })
                             .toSet();
    }

    String name() {
        return element().getSimpleName().toString();
    }
}
