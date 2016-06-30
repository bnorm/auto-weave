package com.bnorm.auto.weave;

import static com.bnorm.auto.weave.AutoAspect.AspectInit;

@AutoAspect(init = AspectInit.SINGLETON)
public enum ValidateAspect {
    instance;

    @AutoAdvice(Validate.class)
    public void before(BeforeJoinPoint point) {
        System.out.println("Before method " + point.method());
    }
}
