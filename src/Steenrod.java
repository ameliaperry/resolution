import java.util.*;

class Sq implements Comparable<Sq>
{
    public static final Sq ID = new Sq(new int[] {});
    public static final Sq BETA = new Sq(new int[] {1});
    public static final Sq P1 = new Sq(new int[] {Config.Q});
    public static final Sq P2 = new Sq(new int[] {2*Config.Q});

    int[] q; /* Indices of the power operations.
                Mod 2, i indicates Sq^i.
                Mod p>2, 2k(p-1) indicates P^i, 2k(p-1)+1 indicates B P^i. */


    public Sq(int[] qq) { q = qq; }

    
    public boolean containsBeta()
    {
        for(int i : q)
            if(i % Config.P != 0)
                return true;
        return false;
    }

    public int deg()
    {
        int deg = 0;
        for(int i : q)
            deg += i;
        return deg;
    }

    public ModSet<Sq> times(Sq o)
    {
        int[] ret = new int[q.length + o.q.length];
        for(int i = 0; i < q.length; i++)
            ret[i] = q[i];
        for(int i = 0; i < o.q.length; i++)
            ret[q.length + i] = o.q[i];

        if(Config.P == 2 && !Config.MICHAEL_MODE)
            return new Sq(ret).resolve_2();
        else
            return new Sq(ret).resolve_p();
    }

    private ModSet<Sq> resolve_2()
    {
        ModSet<Sq> ret;

        ret = new ModSet<Sq>();

        for(int i = q.length - 2; i >= 0; i--) {
            int a = q[i];
            int b = q[i+1];

            if(a >= 2 * b)
                continue;

            /* apply Adem relation */
            for(int c = 0; c <= a/2; c++) {

                if(! ResMath.binom_2(b - c - 1, a - 2*c))
                    continue;

                int[] t;
                if(c == 0) {
                    t = Arrays.copyOf(q, q.length - 1);
                    for(int k = i+2; k < q.length; k++)
                        t[k-1] = q[k];
                    t[i] = a+b-c;
                } else {
                    t = Arrays.copyOf(q, q.length);
                    t[i] = a+b-c;
                    t[i+1] = c;
                }

                /* recurse */
                for(Map.Entry<Sq,Integer> sub : new Sq(t).resolve_2().entrySet())
                    ret.add(sub.getKey(), sub.getValue());
            }

            return ret;
        }

        /* all clear */
        ret.add(this, 1);
        return ret;
    }

    private ModSet<Sq> resolve_p()
    {
        ModSet<Sq> ret;

        ret = new ModSet<Sq>();
        
        int Q = 2 * (Config.P - 1); /* convenience */

        for(int i = q.length - 2; i >= 0; i--) {
            int x = q[i];
            int y = q[i+1];

            if(x >= Config.P * y)
                continue;

            /* apply Adem relation */
            int a = x / Q;
            int b = y / Q;
            int rx = x % Q;
            int ry = y % Q;

            for(int c = 0; c <= a/Config.P; c++) {

                int sign = 1 - 2 * ((a+c) % 2);

//                System.out.printf("adem: x=%d y=%d a=%d b=%d sign=%d\n", x, y, a, b, sign);

                if(rx == 0 && ry == 0)
                    resolve_p_add_term(sign*ResMath.binom_p((Config.P-1)*(b-c)-1,a-c*Config.P), (a+b-c)*Q, c*Q, i, ret);
                else if(rx == 1 && ry == 0)
                    resolve_p_add_term(sign*ResMath.binom_p((Config.P-1)*(b-c)-1,a-c*Config.P), (a+b-c)*Q+1, c*Q, i, ret);
                else if(rx == 0 && ry == 1) {
                    resolve_p_add_term(sign*ResMath.binom_p((Config.P-1)*(b-c),a-c*Config.P), (a+b-c)*Q+1, c*Q, i, ret);
                    resolve_p_add_term(-sign*ResMath.binom_p((Config.P-1)*(b-c)-1,a-c*Config.P-1), (a+b-c)*Q, c*Q+1, i, ret);
                }
                else if(rx == 1 && ry == 1)
                    resolve_p_add_term(-sign*ResMath.binom_p((Config.P-1)*(b-c)-1,a-c*Config.P-1), (a+b-c)*Q+1, c*Q+1, i, ret);
                else Main.die_if(true, "Bad Adem case.");
                       
            }

            return ret;
        }

        /* all clear */
        ret.add(this, 1);
        return ret;
    }

    private void resolve_p_add_term(int coeff, int a, int b, int i, ModSet<Sq> ret)
    {
//        System.out.printf("adem_term: coeff=%d a=%d b=%d\n", coeff, a, b);

        coeff = ResMath.dmod(coeff);
        if(coeff == 0) return; /* save some work... */

        int[] t;
        if(b == 0) {
            t = Arrays.copyOf(q, q.length - 1);
            for(int k = i+2; k < q.length; k++)
                t[k-1] = q[k];
            t[i] = a;
        } else {
            t = Arrays.copyOf(q, q.length);
            t[i] = a;
            t[i+1] = b;
        }

        /* recurse */
        for(Map.Entry<Sq,Integer> sub : new Sq(t).resolve_p().entrySet())
            ret.add(sub.getKey(), sub.getValue() * coeff);
    }

    @Override public String toString()
    {
        if(q.length == 0) return "1";
        String s = "";
        if(Config.P == 2 && ! Config.MICHAEL_MODE) {
            for(int i : q) s += "Sq"+i;
        } else {
            for(int i : q) {
                if(i == 1)
                    s += "\u03b2"; /* beta */
                else if(i % Config.Q == 0)
                    s += "P"+(i/Config.Q);
                else if(i % Config.Q == 1)
                    s += "\u03b2P"+(i/Config.Q);
                else
                    Main.die_if(true, "bad A_"+Config.P+" element: Sq"+i);
            }
        }
        return s;
    }

    @Override public int hashCode()
    {
        int hash = 0;
        for(int i : q)
            hash = hash * 27863521 + i;
        return hash;
    }

    @Override public boolean equals(Object o)
    {
        Sq s = (Sq)o;
        if(q.length != s.q.length)
            return false;
        for(int i = 0; i < q.length; i++)
            if(q[i] != s.q[i])
                return false;
        return true;
    }

    @Override public int compareTo(Sq o)
    {
        if(q.length != o.q.length)
            return q.length - o.q.length;
        for(int i = 0; i < q.length; i++)
            if(q[i] != o.q[i])
                return q[i] - o.q[i];
        return 0;
    }


    /* The Steenrod algebra. */
    public static Iterable<Sq> steenrod(int n)
    {
        Iterable<int[]> p;
        if(Config.P == 2) p = part_2(n,n);
        else            p = part_p(n,n);
        Collection<Sq> ret = new ArrayList<Sq>();

        for(int[] q : p)
            ret.add(new Sq(q));

        return ret;
    }

/*    public static void init()
    {
        int idx = 1;
        for(int n = 0; n < T_CAP; n++)
            for(Sq q : steenrod(n))
                idxcache.put(q, idx);
    } */

    private static Map<String,Iterable<int[]>> part_cache = new HashMap<String,Iterable<int[]>>();
    private static String part_cache_keystr(int n, int max) {
        return "("+n+"/"+max+"/"+Config.P+")";
    }

    private static Iterable<int[]> part_2(int n, int max)
    {
        if(n == 0) { /* the trivial solution */
            Collection<int[]> ret = new ArrayList<int[]>();
            ret.add(new int[] {});
            return ret;
        }
        if(max == 0) return new ArrayList<int[]>(); /* no solutions */
        Iterable<int[]> ret0 = part_cache.get(part_cache_keystr(n,max));
        if(ret0 != null) return ret0;

        Collection<int[]> ret = new ArrayList<int[]>();

        for(int i = (n+1)/2; i <= max; i++) {
            for(int[] q0 : part_2(n-i, i/2)) {
                int[] q1 = new int[q0.length + 1];
                q1[0] = i;
                for(int j = 0; j < q0.length; j++)
                    q1[j+1] = q0[j];
                ret.add(q1);
            }
        }

        part_cache.put(part_cache_keystr(n,max), ret);

        return ret;
    }

    private static Iterable<int[]> part_p(int n, int max)
    {
        if(n == 0) { /* the trivial solution */
            Collection<int[]> ret = new ArrayList<int[]>();
            ret.add(new int[] {});
            return ret;
        }
        if(max == 0) return new ArrayList<int[]>(); /* no solutions */
        Iterable<int[]> ret0 = part_cache.get(part_cache_keystr(n,max));
        if(ret0 != null) return ret0;

        Collection<int[]> ret = new ArrayList<int[]>();

        for(int i = 0; i <= max; i += 2 * (Config.P - 1)) { /* XXX i could start higher? */
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

        part_cache.put(part_cache_keystr(n,max), ret);

        return ret;
    }
}
