package com.bnorm.auto.weave;

@AutoWeave
public abstract class Target {

    @Trace
    public String method() {
        return "Method!";
    }
}
