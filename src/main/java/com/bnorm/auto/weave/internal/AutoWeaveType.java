package com.bnorm.auto.weave.internal;

import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class AutoWeaveType {

    public static AutoWeaveType create(TypeElement type, List<TypeElement> aspects, List<AutoWeaveMethod> methods) {
        return new AutoValue_AutoWeaveType(type, aspects, methods);
    }

    public abstract TypeElement type();

    public abstract List<TypeElement> aspects();

    public abstract List<AutoWeaveMethod> methods();
}
