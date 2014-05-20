package res.algebra;

import res.*;
import java.util.*;

/* The Steenrod algebra, terms of excess at most K. */
public class ExcessModule extends GradedModule<Sq>
{
    private final int K;

    GradedAlgebra<Sq> alg;
    Generator<Sq> g;

    public ExcessModule(int k, GradedAlgebra<Sq> alg)
    {
        /* XXX this should follow the number of extra gradings on alg */
        g = new Generator<Sq>(new int[] {-1,0}, 0);
        this.alg = alg;
        this.K = k;
    }

    @Override public Iterable<Dot<Sq>> basis(int n)
    {
        Collection<Dot<Sq>> ret = new ArrayList<Dot<Sq>>();
        for(int[] q : SteenrodAlgebra.part_p(n,n)) {
            Sq s = new Sq(q);
            if(s.excess() <= K)
                ret.add(new Dot<Sq>(g, s)); 
        }

        return ret;
    } 

    @Override public DModSet<Sq> act(Dot<Sq> a, Sq b)
    {
        ModSet<Sq> prelim = alg.times(a.sq, b);
        DModSet<Sq> ret = new DModSet<Sq>();
        for(Map.Entry<Sq,Integer> ent : prelim.entrySet()) 
            if(ent.getKey().excess() <= K)
                ret.add(new Dot<Sq>(g, ent.getKey()), ent.getValue());
        return ret;
    }

}
