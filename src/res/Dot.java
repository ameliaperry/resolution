package res;

import res.algebra.*;
import java.util.*;

public class Dot<T extends GradedElement<T>> implements Comparable<Dot<T>>
{
    static int id_count = 0;

    /* kernel basis vector */
    public T sq;
    public Generator<T> base;
    public int s;
    public int t;
    public int idx = -1;
    public int nov = -1;

    public Dot(Generator<T> base, T sq) { /* square dot */
        this.base = base;
        this.sq = sq;
        s = base.s;
        t = base.t + sq.deg();
        nov = base.nov + sq.nov();
    }

    @Override public int hashCode()
    {
        int hash = sq.hashCode();
        hash *= 27863521;
        hash ^= t;
        hash *= 27863521;
        hash ^= base.idx;
        return hash;
    }
    @Override public String toString()
    {
        String ret = sq.toString() + "(" + base.t + ";" + base.idx + ")";
        if(nov != -1)
            ret += "(n=" + nov + ")";
        return ret;
    }
    @Override public boolean equals(Object o)
    {
        Dot<?> d = (Dot<?>) o;
        return (d.base.t == base.t && d.base.idx == base.idx && d.sq.equals(sq));
    }

    /*
     * this comparator is only valid for dots in the same bidegree.
     */
    @Override public int compareTo(Dot<T> o)
    {
        /* XXX tweak this for performance */
//        if(nov != -1 && o.nov != -1 && nov != o.nov)
//            return o.nov - nov;
        if(base.t != o.base.t)
            return base.t - o.base.t;
        if(base.idx != o.base.idx)
            return o.base.idx - base.idx;
        return sq.compareTo(o.sq);
    }
}

