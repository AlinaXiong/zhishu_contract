package com.hero.middleware.context;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;

public final class ApiLogTaskContext {

    private static final ThreadLocal<Deque<String>> TASK_ID_STACK = new ThreadLocal<>();

    private ApiLogTaskContext() {
    }

    public static void enter(String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return;
        }
        Deque<String> taskIds = TASK_ID_STACK.get();
        if (taskIds == null) {
            taskIds = new ArrayDeque<>();
            TASK_ID_STACK.set(taskIds);
        }
        taskIds.push(taskId);
    }

    public static void exit() {
        Deque<String> taskIds = TASK_ID_STACK.get();
        if (taskIds == null || taskIds.isEmpty()) {
            TASK_ID_STACK.remove();
            return;
        }
        taskIds.pop();
        if (taskIds.isEmpty()) {
            TASK_ID_STACK.remove();
        }
    }

    public static String currentTaskId() {
        Deque<String> taskIds = TASK_ID_STACK.get();
        return taskIds == null || taskIds.isEmpty() ? null : taskIds.peek();
    }

    public static Runnable wrap(Runnable task) {
        Deque<String> capturedTaskIds = snapshot();
        return () -> {
            Deque<String> previousTaskIds = snapshot();
            restore(capturedTaskIds);
            try {
                task.run();
            } finally {
                restore(previousTaskIds);
            }
        };
    }

    public static <T> Callable<T> wrap(Callable<T> task) {
        Deque<String> capturedTaskIds = snapshot();
        return () -> {
            Deque<String> previousTaskIds = snapshot();
            restore(capturedTaskIds);
            try {
                return task.call();
            } finally {
                restore(previousTaskIds);
            }
        };
    }

    private static Deque<String> snapshot() {
        Deque<String> taskIds = TASK_ID_STACK.get();
        return taskIds == null ? null : new ArrayDeque<>(taskIds);
    }

    private static void restore(Deque<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            TASK_ID_STACK.remove();
            return;
        }
        TASK_ID_STACK.set(new ArrayDeque<>(taskIds));
    }
}
