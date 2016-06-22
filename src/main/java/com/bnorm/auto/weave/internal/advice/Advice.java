package com.bnorm.auto.weave.internal.advice;

public interface Advice {

    Object call(Chain chain);
}
