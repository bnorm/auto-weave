package com.bnorm.auto.weave.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.Modifier;

import com.bnorm.auto.weave.AfterJoinPoint;
import com.bnorm.auto.weave.AfterReturningJoinPoint;
import com.bnorm.auto.weave.AfterThrowingJoinPoint;
import com.bnorm.auto.weave.AroundJoinPoint;
import com.bnorm.auto.weave.BeforeJoinPoint;
import com.bnorm.auto.weave.JoinPoint;
import com.bnorm.auto.weave.internal.advice.Advice;
import com.bnorm.auto.weave.internal.advice.AfterAdvice;
import com.bnorm.auto.weave.internal.advice.AfterReturningAdvice;
import com.bnorm.auto.weave.internal.advice.AfterThrowingAdvice;
import com.bnorm.auto.weave.internal.advice.AroundAdvice;
import com.bnorm.auto.weave.internal.advice.BeforeAdvice;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

enum CrosscutEnum {
    Before(BeforeJoinPoint.class, BeforeAdvice.class, void.class),
    After(AfterJoinPoint.class, AfterAdvice.class, void.class),
    AfterReturning(AfterReturningJoinPoint.class, AfterReturningAdvice.class, void.class),
    AfterThrowing(AfterThrowingJoinPoint.class, AfterThrowingAdvice.class, void.class),
    Around(AroundJoinPoint.class, AroundAdvice.class, Object.class),

    // End of enumeration
    ;

    public static final Map<String, CrosscutEnum> crosscutMap;

    static {
        HashMap<String, CrosscutEnum> temp = new HashMap<>();
        for (CrosscutEnum crosscut : CrosscutEnum.values()) {
            temp.put(crosscut.joinPoint.getName(), crosscut);
        }
        crosscutMap = Collections.unmodifiableMap(temp);
    }

    protected final Class<? extends JoinPoint> joinPoint;
    protected final Class<? extends Advice> chain;
    protected final Class<?> returnType;
    protected final String lowerCaseName;

    CrosscutEnum(Class<? extends JoinPoint> joinPoint, Class<? extends Advice> chain, Class<?> returnType) {
        this.joinPoint = joinPoint;
        this.chain = chain;
        this.returnType = returnType;
        this.lowerCaseName = Names.classToVariable(name());
    }

    public TypeSpec getAdvice(String aspectFieldName, String aspectMethodName) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(lowerCaseName);
        methodBuilder.addAnnotation(Override.class);
        methodBuilder.addModifiers(Modifier.PUBLIC);
        methodBuilder.returns(returnType);
        methodBuilder.addParameter(joinPoint, "joinPoint");
        methodBuilder.addStatement((returnType == void.class ? "" : "return ") + "$N.$N(joinPoint)", aspectFieldName,
                                   aspectMethodName);
        MethodSpec around = methodBuilder.build();

        TypeSpec.Builder chainBuilder = TypeSpec.anonymousClassBuilder("");
        chainBuilder.addSuperinterface(chain);
        chainBuilder.addMethod(around);
        return chainBuilder.build();
    }
}
