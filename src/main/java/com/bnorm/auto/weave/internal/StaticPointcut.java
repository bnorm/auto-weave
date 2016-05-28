package com.bnorm.auto.weave.internal;

import java.util.Arrays;
import java.util.List;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;

import com.google.auto.value.AutoValue;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.TypeName;

@AutoValue
public abstract class StaticPointcut {

    public static StaticPointcut create(String method, Class<?> targetType, Class<?> returnType,
                                        List<Class<?>> argTypes) {
        return new AutoValue_StaticPointcut(method, targetType, returnType, argTypes);
    }

    public static FieldSpec spec(NameAllocator nameAllocator, Types types, WeaveDescriptor classDescriptor,
                                 WeaveMethodDescriptor methodDescriptor) {
        // overload methods?
        String method = methodDescriptor.name();
        TypeName targetType = TypeName.get(classDescriptor.element().asType());
        TypeName returnType = TypeName.get(types.erasure(methodDescriptor.element().getReturnType()));
        List<? extends VariableElement> parameters = methodDescriptor.element().getParameters();

        FieldSpec.Builder pointcutBuilder = FieldSpec.builder(StaticPointcut.class,
                                                              nameAllocator.newName(method + "Pointcut"));
        pointcutBuilder.addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);

        StringBuilder format = new StringBuilder();
        format.append("$T.create($S, $T.class, $T.class, $T.<Class<?>>asList(");
        for (int i = 0, len = parameters.size(); i < len; i++) {
            if (i != 0) {
                format.append(", ");
            }
            format.append("$T.class");
        }
        format.append("))");

        Object[] args = new Object[5 + parameters.size()];
        args[0] = StaticPointcut.class;
        args[1] = method;
        args[2] = targetType;
        args[3] = returnType;
        args[4] = Arrays.class;
        for (int i = 0; i < parameters.size(); i++) {
            args[i + 5] = TypeName.get(types.erasure(parameters.get(i).asType()));
        }

        pointcutBuilder.initializer(format.toString(), args);
        return pointcutBuilder.build();
    }

    public abstract String method();

    public abstract Class<?> targetType();

    public abstract Class<?> returnType();

    public abstract List<Class<?>> argTypes();
}
