package res.algebra;

import java.util.*;

public class Dot<T extends GradedElement<T>> implements Comparable<Dot<T>>
{
    static int id_count = 0;

    /* kernel basis vector */
    public T sq;
    public Generator<T> base;
    public int[] deg;
    public int idx = -1;

    public Dot(Generator<T> base, T sq) { /* square dot */
        this.base = base;
        this.sq = sq;
        deg = Arrays.copyOf(base.deg, base.deg.length);
        deg[1] += sq.deg();
        int[] ex = sq.extraGrading();
        for(int i = 0; i < ex.length; i++)
            deg[i+2] += ex[i];
    }
    
    @Override public String toString()
    {
        String ret = sq.toString() + "(" + base.deg[1] + ";" + base.idx + ")";
        if(deg.length > 2) {
            ret += "(";
            for(int g = 2; g < deg.length; g++) {
                if(g != 2) ret += ",";
                ret += deg[g];
            }
            ret += ")";
        }
        return ret;
    }
    @Override public boolean equals(Object o)
    {
        Dot<?> d = (Dot<?>) o;
        return (d.base.deg[1] == base.deg[1] && d.base.idx == base.idx && d.sq.equals(sq));
    }

    /*
     * this comparator is only valid for dots in the same bidegree.
     */
    @Override public int compareTo(Dot<T> o)
    {
        /* XXX tweak this for performance */
//        if(nov != -1 && o.nov != -1 && nov != o.nov)
//            return o.nov - nov;
        if(base.deg[1] != o.base.deg[1])
            return base.deg[1] - o.base.deg[1];
        if(base.idx != o.base.idx)
            return o.base.idx - base.idx;
        return sq.compareTo(o.sq);
    }
}

