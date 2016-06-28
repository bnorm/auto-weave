package com.bnorm.auto.weave;

@AutoAspect(init = AutoAspect.Initialization.CLASS)
public class ValidateAspect {

    @AutoAdvice(Validate.class)
    public void before(BeforeJoinPoint point) {
        System.out.println("Before method " + point.method());
    }
}
