package com.bnorm.auto.weave;

@AutoWeave
public abstract class Target {

    @Trace
    public void method() {
        System.out.println("Method!");
    }
}
