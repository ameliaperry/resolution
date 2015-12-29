package res.algebras;

import res.*;
import res.algebratypes.*;
import java.util.*;

public class Sq implements GradedElement<Sq>
{
    public static final Sq UNIT = new Sq(new int[] {});
    public static final Sq[] HOPF = new Sq[] {
        new Sq(1),
        new Sq(Config.Q),
        new Sq(Config.P*Config.Q),
        new Sq(Config.P*Config.P*Config.Q)
    };


    public int[] q; /* Indices of the power operations.
                Mod 2, i indicates Sq^i.
                Mod p>2, 2i(p-1) indicates P^i, 2i(p-1)+1 indicates B P^i. */
    public int[] deg;


    public Sq(int[] qq) { q = qq; }
    public Sq(int qq) { this(new int[] {qq}); }
    
    private static final int[] EMPTY = new int[] {};
    private static final int[] ZERO = new int[] {0};
    private static final int[] ONE = new int[] {1};
    private static final int[][] SINGLETONS = new int[10000][1];
    static {
        for(int i = 0; i < 10000; i++) { SINGLETONS[i][0] = i; }
    }

    /* novikov filtration is 1 if there are no betas. this returns 1 for the
     * identity operation; is this okay? */
    @Override public int[] multideg()
    {
        int deg = deg();
        if(Config.MICHAEL_MODE) {
            for(int i : q)
                if(i % Config.P != 0)
                    return new int[] {deg,0};
            return new int[] {deg,1};
        } else if(Config.MOTIVIC_GRADING) {
            int tot = 0;
            for(int a : q) tot += a/2;
            return new int[] {deg,tot};
        } else return SINGLETONS[deg];
    }

    @Override public int deg()
    {
        int deg = 0;
        for(int i : q)
            deg += i;
        return deg;
    }

    public int excess()
    {
        if(q.length == 0) return 0;
        int exc = q[q.length-1];
        for(int i = 1; i < q.length; i++)
            exc += q[i-1] - Config.P * q[i];
        return exc;
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
        
        /* convenience */
        final int P = Config.P;
        final int Q = 2 * (Config.P - 1);
        final int R = Config.P - 1;

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

                int sign = ((a ^ c) & 1) == 0  ?  1  :  -1;

//                System.out.printf("adem: x=%d y=%d a=%d b=%d sign=%d\n", x, y, a, b, sign);

                if(ry == 0)
                    resolve_p_add_term( sign*ResMath.binom_p(R*(b-c)-1,a-c*P  ), (a+b-c)*Q+rx, c*Q  , i, ret);
                else {
                    if(rx == 0) {
                        resolve_p_add_term( sign*ResMath.binom_p(R*(b-c)  ,a-c*P  ), (a+b-c)*Q+1, c*Q  , i, ret);
                        resolve_p_add_term(-sign*ResMath.binom_p(R*(b-c)-1,a-c*P-1), (a+b-c)*Q  , c*Q+1, i, ret);
                    } else
                        resolve_p_add_term(-sign*ResMath.binom_p(R*(b-c)-1,a-c*P-1), (a+b-c)*Q+1, c*Q+1, i, ret);
                }
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

    @Override public String extraInfo() {
        return "";
    }

    @Override public int hashCode()
    {
        int hash = 0;
        for(int i : q)
            hash = hash * 27863521 ^ i;
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

}

