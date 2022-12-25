package com.github.ghik.poligon;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class VarHandler {
    private static long x;

    private static final VarHandle X;

    static {
        try {
            X = MethodHandles.lookup().findStaticVarHandle(VarHandler.class, "x", long.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        while (true) {
            var start = System.nanoTime();
            for (int i = 0; i < 10000000; i++) {
                X.set((long) X.get());
            }
            var dur = System.nanoTime() - start;
            System.out.println(dur / 1000000);
        }
    }
}
