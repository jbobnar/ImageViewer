/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The thread factory to be used in combination with the image executor.
 * 
 * @author Jaka Bobnar
 *
 */
public final class ThreadFactory implements java.util.concurrent.ThreadFactory {
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    /**
     * Constructs a new factory. All threads will have the given name in their names.
     *
     * @param name part of the name of all threads
     */
    ThreadFactory(String name) {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        namePrefix = "pool-" + poolNumber.getAndIncrement() + "-" + name + "-";
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group,r,namePrefix + threadNumber.getAndIncrement(),0);
        t.setDaemon(true);
        return t;
    }
}
