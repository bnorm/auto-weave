package com.bnorm.auto.weave.internal;

public final class Util {
    private Util() {
    }

    /** Java Puzzlers #43. */
    public static Error sneakyThrow(Throwable t) {
        return Util.<Error>sneakyThrowGeneric(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> Error sneakyThrowGeneric(Throwable t) throws T {
        throw (T) t;
    }
}
