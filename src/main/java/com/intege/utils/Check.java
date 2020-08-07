package com.intege.utils;

public abstract class Check {

    public static <T> void notNullArgument(final T var, final String varName) {
        if (var == null) {
            throw new NullPointerException("Variable \"" + varName + "\" is null!");
        }
    }

}
