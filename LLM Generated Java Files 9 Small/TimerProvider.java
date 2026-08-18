package org.keycloak.timer;

import org.keycloak.provider.Provider;

public interface TimerProvider extends Provider {

    void schedule(Runnable runnable, long intervalMillis, String taskName);

    void scheduleTask(ScheduledTask scheduledTask, long intervalMillis, String taskName);

    TimerTaskContext cancelTask(String taskName);

    interface TimerTaskContext {

        Runnable getRunnable();

        long getIntervalMillis();
    }
}
