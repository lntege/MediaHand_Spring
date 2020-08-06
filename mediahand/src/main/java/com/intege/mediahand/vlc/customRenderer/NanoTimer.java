/*
 * This file is part of VLCJ.
 *
 * VLCJ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VLCJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VLCJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014, 2015 Caprica Software Limited.
 */

package com.intege.mediahand.vlc.customRenderer;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;

/**
 * Originally contributed by Jason Pollastrini, with changes.
 */
public abstract class NanoTimer extends ScheduledService<Void> {

    private static final long ONE_NANO = 1000000000L;

    private static final double ONE_NANO_INV = 1f / 1000000000L;

    private long startTime;

    private long previousTime;

    private double frameRate;

    private double deltaTime;

    public NanoTimer(double period) {
        super();
        this.setPeriod(Duration.millis(period));
        this.setExecutor(Executors.newCachedThreadPool(new NanoThreadFactory()));
    }

    public final long getTime() {
        return System.nanoTime() - this.startTime;
    }

    public final double getTimeAsSeconds() {
        return getTime() * NanoTimer.ONE_NANO_INV;
    }

    public final double getDeltaTime() {
        return this.deltaTime;
    }

    public final double getFrameRate() {
        return this.frameRate;
    }

    @Override
    public final void start() {
        super.start();
        if (this.startTime <= 0) {
            this.startTime = System.nanoTime();
        }
    }

    @Override
    public final void reset() {
        super.reset();
        this.startTime = System.nanoTime();
        this.previousTime = getTime();
    }

    @Override
    protected final Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateTimer();
                return null;
            }
        };
    }

    private void updateTimer() {
        this.deltaTime = (getTime() - this.previousTime) * (1.0f / NanoTimer.ONE_NANO);
        this.frameRate = 1.0f / this.deltaTime;
        this.previousTime = getTime();
    }

    @Override
    protected final void succeeded() {
        super.succeeded();
        onSucceeded();
    }

    @Override
    protected final void failed() {
        getException().printStackTrace(System.err);
        onFailed();
    }

    private static class NanoThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "NanoTimerThread");
            thread.setPriority(Thread.NORM_PRIORITY + 1);
            thread.setDaemon(true);
            return thread;
        }
    }

    protected abstract void onSucceeded();

    protected void onFailed() {
    }
}
