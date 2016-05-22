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
import com.bnorm.auto.weave.internal.chain.AroundChain;
import com.bnorm.auto.weave.internal.chain.BeforeChain;
import com.bnorm.auto.weave.internal.chain.Chain;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

enum CrosscutEnum {
    Before(BeforeJoinPoint.class, BeforeChain.class, void.class),
    After(AfterJoinPoint.class, null, void.class),
    AfterReturning(AfterReturningJoinPoint.class, null, void.class),
    AfterThrowing(AfterThrowingJoinPoint.class, null, void.class),
    Around(AroundJoinPoint.class, AroundChain.class, Object.class),

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
    protected final Class<? extends Chain> chain;
    protected final Class<?> returnType;
    protected final String lowerCaseName;

    CrosscutEnum(Class<? extends JoinPoint> joinPoint, Class<? extends Chain> chain, Class<?> returnType) {
        this.joinPoint = joinPoint;
        this.chain = chain;
        this.returnType = returnType;
        this.lowerCaseName = name().toLowerCase();
    }

    public TypeSpec getChain(String pointcut, String aspectFieldName, String aspectMethodName) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(lowerCaseName);
        methodBuilder.addAnnotation(Override.class);
        methodBuilder.addModifiers(Modifier.PUBLIC);
        if (returnType != void.class) {
            methodBuilder.addException(Throwable.class);
        }
        methodBuilder.returns(returnType);
        methodBuilder.addParameter(joinPoint, "joinPoint");
        methodBuilder.addStatement((returnType == void.class ? "" : "return ") + "$N.$N(joinPoint)", aspectFieldName,
                                   aspectMethodName);
        MethodSpec around = methodBuilder.build();

        TypeSpec.Builder chainBuilder = TypeSpec.anonymousClassBuilder("chain, " + pointcut);
        chainBuilder.addSuperinterface(chain);
        chainBuilder.addMethod(around);
        return chainBuilder.build();
    }
}
