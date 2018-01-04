/*
 * (C) Copyright 2016 Jaka Bobnar. All rights reserved.
 */
package com.jakabobnar.imageviewer.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ImageExecutor is a thread pool which uses a fixed number of threads and a specific queue. This executor prints the
 * stack trace of any exception that happens during execution.
 *
 * @author Jaka Bobnar
 *
 */
public class ImageExecutor extends ThreadPoolExecutor {

    /**
     * Constructs a new executor.
     *
     * @param name the name to use for the threads created by the executor
     * @param size the number of threads to use
     * @param queue the queue to use
     */
    public ImageExecutor(String name, int size, BlockingQueue<Runnable> queue) {
        super(size,size,0L,TimeUnit.MILLISECONDS,queue,new ThreadFactory(name),(r, t) -> {/*nothing*/});
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.concurrent.ThreadPoolExecutor#afterExecute(java.lang.Runnable, java.lang.Throwable)
     */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        if (t != null) {
            t.printStackTrace();
        }
    }
}
