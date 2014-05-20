package res.algebra;

import res.*;
import java.util.*;

public class CofibHopf extends GradedModule<Sq>
{
    private int i;
    private Dot<Sq> d1, d2;

    public CofibHopf(int i, GradedAlgebra<Sq> alg)
    {
        this.i = i;
        /* XXX should follow the number of extra gradings on alg */
        Generator<Sq> g = new Generator<Sq>(new int[] {-1,0,0}, 0);
        d1 = new Dot<Sq>(g, Sq.UNIT);
        d2 = new Dot<Sq>(g, Sq.HOPF[i]);
    }

    @Override public Iterable<Dot<Sq>> basis(int deg)
    {
        if(deg == 0) return Collections.singleton(d1);
        if(deg == d2.deg[1]) return Collections.singleton(d2);
        return Collections.emptySet();
    }

    @Override public DModSet<Sq> act(Dot<Sq> o, Sq sq)
    {
        DModSet<Sq> ret = new DModSet<Sq>();
        if(o.deg[1] == d1.deg[1] && sq.equals(Sq.UNIT))
            ret.add(d1,1);
        if(o.deg[1] == d2.deg[1] && sq.equals(Sq.UNIT))
            ret.add(d2,1);
        if(o.deg[1] == d1.deg[1] && sq.equals(Sq.HOPF[i]))
            ret.add(d2,1);
        return ret;
    }
}

