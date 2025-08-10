package com.myceliumbot;

import net.dv8tion.jda.api.JDA;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler {
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(5);
    private final ScriptManager scriptManager;
    private final JDA jda;

    public Scheduler(ScriptManager scriptManager, JDA jda) {
        this.scriptManager = scriptManager;
        this.jda = jda;
    }

    public void scheduleOnce(String scriptFileName, String handlerName, long delay, String timeUnit) {
        Runnable task = () -> scriptManager.executeScheduledTask(scriptFileName, handlerName, jda);
        executorService.schedule(task, delay, TimeUnit.valueOf(timeUnit.toUpperCase()));
        System.out.printf("Scheduled task '%s' in '%s' to run once in %d %s.%n", handlerName, scriptFileName, delay, timeUnit);
    }

    public void scheduleRepeating(String scriptFileName, String handlerName, long initialDelay, long period, String timeUnit) {
        Runnable task = () -> scriptManager.executeScheduledTask(scriptFileName, handlerName, jda);
        executorService.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.valueOf(timeUnit.toUpperCase()));
        System.out.printf("Scheduled task '%s' in '%s' to run every %d %s.%n", handlerName, scriptFileName, period, timeUnit);
    }
}
