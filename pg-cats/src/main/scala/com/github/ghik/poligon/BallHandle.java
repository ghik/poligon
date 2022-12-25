package com.github.ghik.poligon;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public abstract class BallHandle extends Thread {
    private long p00;
    private long p01;
    private long p02;
    private long p03;
    private long p04;
    private long p05;
    private long p06;
    private long p07;

    private volatile long step;
    private Ball ball;

    private long p10;
    private long p11;
    private long p12;
    private long p13;
    private long p14;
    private long p15;
    private long p16;
    private long p17;

    private static final VarHandle STEP;

    static {
        try {
            STEP = MethodHandles.lookup().findVarHandle(BallHandle.class, "step", long.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public long readStep() {
        return (long) STEP.getAcquire(this);
    }

    public void nextStep(long step) {
        STEP.setRelease(this, step);
    }
}
