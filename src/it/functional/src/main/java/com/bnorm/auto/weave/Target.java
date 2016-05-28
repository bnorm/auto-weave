package com.bnorm.auto.weave;

import java.io.IOException;

@AutoWeave
public abstract class Target {

    public static Target create() {
        return new AutoWeave_Target();
    }

    @Trace
    public String method(Integer i) throws IOException {
        return "Method!";
    }
}
