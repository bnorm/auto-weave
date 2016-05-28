package test;

import com.bnorm.auto.weave.AroundJoinPoint;
import com.bnorm.auto.weave.AutoAdvice;

class TraceAspect {
    @AutoAdvice(Trace.class)
    public Object around(AroundJoinPoint point) {
        return point.proceed();
    }
}
