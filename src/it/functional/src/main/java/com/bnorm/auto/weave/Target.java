package com.bnorm.auto.weave;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@AutoWeave
public abstract class Target {

    public static Target create() {
        return new AutoWeave_Target();
    }

    @Trace
    public Future<AtomicBoolean> doSomething(List<AtomicInteger> i) throws IOException {
        return new FutureTask<AtomicBoolean>(new Runnable() {
            @Override
            public void run() {
            }
        }, new AtomicBoolean());
    }
}
