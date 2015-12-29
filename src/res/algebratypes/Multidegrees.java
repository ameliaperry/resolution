package res.algebratypes;

import java.util.*;

public final class Multidegrees
{
    public static final Comparator<int[]> multidegComparator = new Comparator<int[]> () {
        @Override public int compare(int[] a, int[] b) {
            int[] small,big;
            int sign;
            if(a.length < b.length) {
                small = a;
                big = b;
                sign = -1;
            } else {
                small = b;
                big = a;
                sign = 1;
            }

            for(int i = 0; i < big.length; i++) {
                if(i < small.length) {
                    if(a[i] != b[i])
                        return a[i] - b[i];
                } else {
                    if(big[i] != 0) {
                        return sign;
                    }
                }
            }
            return 0;
        }
    };

    public static int[] minkey(int[] req, int len) {
        return req;
    }
    public static int[] maxkey(int[] req, int len) {
        if(len <= req.length)
            return req;
        int[] ret = new int[len];
        for(int i = 0; i < req.length; i++)
            ret[i] = req[i];
        for(int i = req.length; i < len; i++)
            ret[i] = Integer.MAX_VALUE;
        return ret;
    }

    public static int[] sumdeg(int[] d1, int[] d2) {
        int maxlen = (d1.length > d2.length) ? d1.length : d2.length;
        int[] ret = Arrays.copyOf(d1, maxlen);
        for(int i = 0; i < d2.length; i++)
            ret[i] += d2[i];
        return ret;
    }
}

