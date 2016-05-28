package test;

import com.bnorm.auto.weave.AroundJoinPoint;
import com.bnorm.auto.weave.internal.Pointcut;
import com.bnorm.auto.weave.internal.StaticPointcut;
import com.bnorm.auto.weave.internal.chain.AroundChain;
import com.bnorm.auto.weave.internal.chain.Chain;
import com.bnorm.auto.weave.internal.chain.MethodChain;
import com.bnorm.auto.weave.internal.chain.MethodException;
import java.lang.AssertionError;
import java.lang.Error;
import java.lang.Object;
import java.lang.Override;
import java.lang.RuntimeException;
import java.lang.String;
import java.lang.Throwable;
import java.util.Arrays;

final class AutoWeave_Target extends Target {
    private static final StaticPointcut methodPointcut = StaticPointcut.create("method", Target.class, String.class, Arrays.<Class<?>>asList());

    private final TraceAspect traceAspect = new TraceAspect();

    AutoWeave_Target() {
        super();
    }

    @Override
    @Trace
    public String method() {
        Pointcut pointcut = Pointcut.create(this, Arrays.<Object>asList(), methodPointcut);
        Chain chain;
        chain = new MethodChain() {
            @Override
            public Object method() throws Throwable {
                return AutoWeave_Target.super.method();
            }
        };
        chain = new AroundChain(chain, pointcut) {
            @Override
            public Object around(AroundJoinPoint joinPoint) {
                return traceAspect.around(joinPoint);
            }
        };
        try {
            return (String) chain.call();
        } catch (MethodException e) {
            if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            } else if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new AssertionError("Please contact the library developer", e.getCause());
            }
        }
    }
}
