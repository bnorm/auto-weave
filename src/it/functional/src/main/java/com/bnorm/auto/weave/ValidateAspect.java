package com.bnorm.auto.weave;

public class ValidateAspect {

    @AutoAdvice(Validate.class)
    public void before(BeforeJoinPoint point) {
        System.out.println("Before method " + point.method());
    }
}
