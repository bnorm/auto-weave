package test;

import com.bnorm.auto.weave.AroundJoinPoint;
import com.bnorm.auto.weave.internal.StaticPointcut;
import com.bnorm.auto.weave.internal.Util;
import com.bnorm.auto.weave.internal.advice.Advice;
import com.bnorm.auto.weave.internal.advice.AroundAdvice;
import com.bnorm.auto.weave.internal.advice.Chain;
import com.bnorm.auto.weave.internal.advice.MethodException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.Throwable;
import java.util.Arrays;

final class AutoWeave_Target extends Target {
    private static final StaticPointcut methodPointcut = StaticPointcut.create("method", Target.class, String.class, Arrays.<Class<?>>asList());

    private final TraceAspect traceAspect = new TraceAspect();

    private final Advice[] methodAdvice = new Advice[]{
            new AroundAdvice() {
                @Override
                public Object around(AroundJoinPoint joinPoint) {
                    return traceAspect.around(joinPoint);
                }
            }
    };

    AutoWeave_Target() {
        super();
    }

    @Override
    @Trace
    public String method() {
        try {
            return (String) new Chain(methodAdvice, this, methodPointcut, Arrays.<Object>asList()) {
                @Override
                public Object call() throws Throwable {
                    return AutoWeave_Target.super.method();
                }
            }.proceed();
        } catch (MethodException e) {
            throw Util.sneakyThrow(e.getCause());
        }
    }
}
