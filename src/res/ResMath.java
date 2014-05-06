package res;

import java.util.*;

/*
 * little arithmetic functions like modular arithmetic
 */
public final class ResMath
{
    public static int[] inverse;

    public static boolean binom_2(int a, int b)
    {
        return ((~a) & b) == 0;
    }

    private static Map<Integer,Integer> binom_cache = new TreeMap<Integer,Integer>();
    private static Integer binom_cache_key(int a, int b) { return (a<<16) | b; }
    public static int binom_p(int a, int b)
    {
        Integer s = binom_cache_key(a,b);
        Integer i = binom_cache.get(s);
        if(i != null) return i;

        int ret;
        if(a < 0 || b < 0 || b > a)
            ret = 0;
        else if(a == 0)
            ret = 1;
        else ret = dmod(binom_p(a-1,b) + binom_p(a-1,b-1));

        binom_cache.put(s,ret);
        return ret;
    }

    public static int dmod(int n)
    {
        return (n + (Config.P << 8)) % Config.P;
    }

    public static int floorstep(int n, int m)
    {
        return (n / m) * m;
    }

    static void calcInverses()
    {
        inverse = new int[Config.P];
        for(int i = 1; i < Config.P; i++) {
            for(int j = 1; j < Config.P; j++) {
                if((i * j) % Config.P == 1) {
                    inverse[i] = j;
                    break;
                }
            }
        }
    }

}
