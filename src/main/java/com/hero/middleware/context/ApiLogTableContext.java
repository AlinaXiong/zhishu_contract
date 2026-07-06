package com.hero.middleware.context;

import java.util.concurrent.Callable;

public final class ApiLogTableContext {

    private static final ThreadLocal<Integer> SUPPRESS_DEPTH = new ThreadLocal<>();

    private ApiLogTableContext() {
    }

    public static void enterSuppressed() {
        Integer depth = SUPPRESS_DEPTH.get();
        SUPPRESS_DEPTH.set(depth == null ? 1 : depth + 1);
    }

    public static void exitSuppressed() {
        Integer depth = SUPPRESS_DEPTH.get();
        if (depth == null || depth <= 1) {
            SUPPRESS_DEPTH.remove();
            return;
        }
        SUPPRESS_DEPTH.set(depth - 1);
    }

    public static boolean isSuppressed() {
        Integer depth = SUPPRESS_DEPTH.get();
        return depth != null && depth > 0;
    }

    public static <T> Callable<T> wrap(Callable<T> task) {
        boolean suppressed = isSuppressed();
        return () -> {
            if (!suppressed) {
                return task.call();
            }
            enterSuppressed();
            try {
                return task.call();
            } finally {
                exitSuppressed();
            }
        };
    }
}
