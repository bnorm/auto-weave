package com.bnorm.auto.weave.internal;

import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

import com.google.auto.value.AutoValue;
import com.squareup.javapoet.FieldSpec;

@AutoValue
public abstract class StaticPointcut {

    public static StaticPointcut create(String method, Class<?> targetType, Class<?> returnType,
                                        List<Class<?>> argTypes) {
        return new AutoValue_StaticPointcut(method, targetType, returnType, argTypes);
    }

    public static FieldSpec spec(WeaveDescriptor classDescriptor, WeaveMethodDescriptor methodDescriptor) {
        FieldSpec.Builder pointcutBuilder = FieldSpec.builder(StaticPointcut.class, methodDescriptor.name() + "Pointcut");
        pointcutBuilder.addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
        String method = methodDescriptor.name();
        String targetType = classDescriptor.name() + ".class";
        String returnType = methodDescriptor.element().getReturnType().toString() + ".class";
        StringBuilder argTypes = new StringBuilder();
        List<? extends VariableElement> parameters = methodDescriptor.element().getParameters();
        for (int i = 0, len = parameters.size(); i < len; i++) {
            if (i != 0) {
                argTypes.append(", ");
            }
            argTypes.append(parameters.get(i).asType()).append(".class");
        }
        pointcutBuilder.initializer("$T.create($S, $L, $L, $T.<Class<?>>asList($L))", StaticPointcut.class, method, targetType, returnType,
                                    Arrays.class, argTypes);
        return pointcutBuilder.build();
    }

    public abstract String method();

    public abstract Class<?> targetType();

    public abstract Class<?> returnType();

    public abstract List<Class<?>> argTypes();
}
