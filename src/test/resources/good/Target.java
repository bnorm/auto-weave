package test;

import com.bnorm.auto.weave.AutoWeave;

@AutoWeave
abstract class Target {
    @Trace
    public String method() {
        return "Method!";
    }
}
