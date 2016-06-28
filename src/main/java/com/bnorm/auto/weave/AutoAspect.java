package com.bnorm.auto.weave;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface AutoAspect {

    Initialization init() default Initialization.INSTANCE;

    // todo(bnorm) rename to Init?
    enum Initialization {
        INSTANCE,
        CLASS
        // todo(bnorm) SINGLETON
    }
}
