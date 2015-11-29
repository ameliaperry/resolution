package res.algebras;

import res.algebratypes.*;
import res.utils.*;
import java.util.*;

/* The Steenrod algebra, terms of excess at most K. */
public class ExcessModule extends GradedModule<Sq,Sq>
{
    private final int K;
    private final GradedAlgebra<Sq> alg;

    public ExcessModule(int k, GradedAlgebra<Sq> alg)
    {
        /* XXX the extra-grading behavior is probably very broken */ 
        this.alg = alg;
        this.K = k;
    }

    @Override public Iterable<Sq> basis(int n)
    {
        return new FilterIterable<Sq>(alg.basis(n), new Func<Sq,Boolean>() {
            @Override public Boolean run(Sq s) {
                return s.excess() <= K;
            }
        });
    } 

    @Override public ModSet<Sq> times(Sq a, Sq b)
    {
        ModSet<Sq> prelim = alg.times(a,b);
        ModSet<Sq> ret = new ModSet<Sq>();
        for(Map.Entry<Sq,Integer> ent : prelim.entrySet()) 
            if(ent.getKey().excess() <= K)
                ret.add(ent.getKey(), ent.getValue());
        return ret;
    }
}

