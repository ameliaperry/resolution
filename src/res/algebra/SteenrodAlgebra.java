package res.algebra;

import res.*;
import java.util.*;

/* The Steenrod algebra. */
public class SteenrodAlgebra implements GradedAlgebra<Sq>
{
    public SteenrodAlgebra() {}

    @Override public Iterable<Sq> basis(int n)
    {
        Collection<Sq> ret = new ArrayList<Sq>();
        for(int[] q : part_p(n,n))
            ret.add(new Sq(q));

        return ret;
    }

    @Override public ModSet<Sq> times(Sq a, Sq b)
    {
        return a.times(b);
    }

    @Override public Sq unit()
    {
        return Sq.UNIT;
    }

    @Override public List<Sq> distinguished()
    {
        ArrayList<Sq> ret = new ArrayList<Sq>();
        ret.add(Sq.HOPF[0]);
        ret.add(Sq.HOPF[1]);
        ret.add(Sq.HOPF[2]);
        return ret;
    }

    @Override public int extraDegrees()
    {
        if(Config.MICHAEL_MODE && Config.P == 2) return 1;
        if(Config.MOTIVIC_GRADING) return 1;
        return 0;
    }

    private static Map<Integer,Iterable<int[]>> part_cache = new TreeMap<Integer,Iterable<int[]>>();
    private static Integer part_cache_key(int n, int max) {
        return (Config.P << 28) ^ (n << 14) ^ max;
    }
    private static final Iterable<int[]> ZERO = Collections.emptyList(); /* no solutions */
    private static final Iterable<int[]> ONE = Collections.singleton(new int[] {}); /* the trivial partition of zero */

    /*
     * Returns all partitions of <n> into P-admissible sequences of largest entry at most <max>.
     */
    static Iterable<int[]> part_p(int n, int max)
    {
        /* base cases */
        if(n == 0) return Collections.singleton(new int[] {}); /* the trivial partition */
        if(max == 0) return Collections.emptyList(); /* no solutions */

        /* cache */
        Iterable<int[]> ret0 = part_cache.get(part_cache_key(n,max));
        if(ret0 != null) return ret0;

        Collection<int[]> ret = new ArrayList<int[]>();

        for(int i = n * (Config.P-1) / (Config.P * Config.Q) * Config.Q; i <= max; i += Config.Q) {
            /* try P^i */
            for(int[] q0 : part_p(n-i, i/Config.P)) {
                int[] q1 = new int[q0.length + 1];
                q1[0] = i;
                for(int j = 0; j < q0.length; j++)
                    q1[j+1] = q0[j];
                ret.add(q1);
            }
            /* try BP^i */
            if(i+1 > max) break;
            for(int[] q0 : part_p(n-(i+1), (i+1)/Config.P)) {
                int[] q1 = new int[q0.length + 1];
                q1[0] = i+1;
                for(int j = 0; j < q0.length; j++)
                    q1[j+1] = q0[j];
                ret.add(q1);
            }
        }

        part_cache.put(part_cache_key(n,max), ret);

        return ret;
    }

}
