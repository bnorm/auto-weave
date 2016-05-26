package com.bnorm.auto.weave.internal;

final class Names {
    private Names() {
    }

    /** Converts a class style Java name to a variable style name. */
    public static String classToVariable(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
