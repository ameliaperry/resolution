package res.algebras;

import res.Config;
import res.algebratypes.*;
import res.utils.*;
import java.util.*;

/* The Steenrod algebra. */
public class SteenrodAlgebra extends AbstractGradedAlgebra<Sq> implements GradedBialgebra<Sq>
{
    public SteenrodAlgebra() {}

    @Override public Iterable<Sq> gens(int n)
    {
        return new MapIterable<int[],Sq>(part_p(n,n), new Func<int[],Sq>() {
            @Override public Sq run(int[] q) {
                return new Sq(q);
            }
        });
    }

    @Override public ModSet<Sq> times(Sq a, Sq b)
    {
        return a.times(b);
    }

    @Override public Sq unit()
    {
        return Sq.UNIT;
    }

    @Override public int counit(Sq sq) {
        if(sq.equals(Sq.UNIT)) return 1;
        return 0;
    }

    @Override public List<Sq> distinguished()
    {
        ArrayList<Sq> ret = new ArrayList<Sq>();
        ret.add(Sq.HOPF[0]);
        ret.add(Sq.HOPF[1]);
        ret.add(Sq.HOPF[2]);
        ret.add(Sq.HOPF[3]);
        return ret;
    }

    private static Map<Integer,Iterable<int[]>> part_cache = new TreeMap<Integer,Iterable<int[]>>();
    private static Integer part_cache_key(int n, int max) {
        return (Config.P << 28) ^ (n << 14) ^ max;
    }
    private static final Iterable<int[]> ZERO = Collections.emptyList(); /* no solutions */
    private static final Iterable<int[]> ONE = Collections.singleton(new int[] {}); /* the trivial partition of zero */

    /*
     * Returns all partitions of <n> into P-admissible sequences of largest entry at most <max>.
     * TODO: make this really iterator-based instead of collection-based, to save memory
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
    
    
    private static Map<Sq,ModSet<Pair<Sq,Sq>>> diagonal_cache = new TreeMap<Sq,ModSet<Pair<Sq,Sq>>>();
    @Override public ModSet<Pair<Sq,Sq>> diagonal(Sq q)
    {
        ModSet<Pair<Sq,Sq>> ret = diagonal_cache.get(q);
        if(ret != null) return ret;

        ret = new ModSet<Pair<Sq,Sq>>();

        if(q.q.length == 0) {
            ret.add(new Pair<Sq,Sq>(Sq.UNIT,Sq.UNIT),1);
            diagonal_cache.put(q,ret);
            return ret;
        }

        if(q.q.length == 1) { /* cartan rule */
            ret.add(new Pair<Sq,Sq>(Sq.UNIT, q), 1);
            for(int i = 1; i < q.q[0]; i++)
                ret.add(new Pair<Sq,Sq>(new Sq(i), new Sq(q.q[0]-i)), 1);
            ret.add(new Pair<Sq,Sq>(q, Sq.UNIT), 1);
            diagonal_cache.put(q,ret);
            return ret;
        }

        /* general case: recurse by multiplication */
        Sq a = new Sq(Arrays.copyOf(q.q, q.q.length-1));
        Sq b = new Sq(q.q[q.q.length-1]);
        ModSet<Pair<Sq,Sq>> da = diagonal(a);
        ModSet<Pair<Sq,Sq>> db = diagonal(b);

        for(Map.Entry<Pair<Sq,Sq>,Integer> ea : da.entrySet()) {
            Pair<Sq,Sq> sqa = ea.getKey();
            for(Map.Entry<Pair<Sq,Sq>,Integer> eb : db.entrySet()) {
                Pair<Sq,Sq> sqb = eb.getKey();
                for(Map.Entry<Sq,Integer> e0 : sqa.a.times(sqb.a).entrySet())
                    for(Map.Entry<Sq,Integer> e1 : sqa.b.times(sqb.b).entrySet())
                        ret.add(new Pair<Sq,Sq>(e0.getKey(), e1.getKey()), 
                                ea.getValue() * eb.getValue() * e0.getValue() * e1.getValue());
            }
        }

        diagonal_cache.put(q,ret);
        return ret;
    }

}

