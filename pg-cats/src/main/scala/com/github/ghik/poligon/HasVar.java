package com.github.ghik.poligon;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public abstract class HasVar {
    private boolean done;
    protected long x;
    protected long iks;

    public static final VarHandle DONE;
    public static final VarHandle X;

    static {
        try {
            DONE = MethodHandles.lookup().findVarHandle(HasVar.class, "done", boolean.class);
            X = MethodHandles.lookup().findVarHandle(HasVar.class, "x", long.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
