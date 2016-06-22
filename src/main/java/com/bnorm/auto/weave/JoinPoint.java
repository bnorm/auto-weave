package com.bnorm.auto.weave;

import java.util.List;

public interface JoinPoint {

    Object target();

    List<?> args();

    String method();

    Class<?> targetType();

    Class<?> returnType();

    List<Class<?>> argTypes();
}
