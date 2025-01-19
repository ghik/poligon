package interview;

import java.util.ArrayList;
import java.util.Random;

class Solution {
    private Random r = new Random();
    private int[] dw;

    public Solution(int[] w) {
        if (w.length == 0) {
            throw new IllegalArgumentException("Empty Array");
        }

        dw = new int[w.length];
        dw[0] = w[0];
        for (int i = 1; i < w.length; i++) {
            dw[i] = dw[i - 1] + w[i];
        }
    }

    public int pickIndex() {
        int total = dw[dw.length - 1];
        int rw = r.nextInt(total);

        int from = 0, to = dw.length;
        while (to > from) {
            int mid = (to + from) / 2;
            if (dw[mid] <= rw) {
                from = mid + 1;
            } else {
                to = mid;
            }
        }

        return from;
    }

    public static void main(String[] args) {
        var arr = new int[5][10];

        var al = new ArrayList<Integer>();

        var s = new Solution(new int[]{1,3});
        System.out.println(s.pickIndex());
    }
}
