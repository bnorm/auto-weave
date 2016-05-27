package com.bnorm.auto.weave;

@AutoWeave
public abstract class Target {

    public static Target create() {
        return new AutoWeave_Target();
    }

    @Trace
    public String method() {
        return "Method!";
    }
}
