package com.bnorm.auto.weave;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface AutoAspect {

    AspectInit init() default AspectInit.INSTANCE;

    enum AspectInit {
        INSTANCE,
        CLASS,
        SINGLETON
    }
}
