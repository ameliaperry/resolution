package res.algebra;

import java.util.Map;

/* to work around generic array restrictions */
public class DModSet<T extends GradedElement<T>> extends ModSet<Dot<T>>
{
    public DModSet() {}
    public DModSet(Dot<T> d) {
        super(d);
    }

    public DModSet<T> times(T sq, GradedAlgebra<T> alg)
    {
        DModSet<T> ret = new DModSet<T>();
        for(Map.Entry<Dot<T>,Integer> e1 : entrySet()) {
            Dot<T> d = e1.getKey();
            ModSet<T> prod = alg.times(sq, d.sq);
            for(Map.Entry<T,Integer> e2 : prod.entrySet())
                ret.add(new Dot<T>(d.base, e2.getKey()), e1.getValue() * e2.getValue());
        }
        return ret;
    }
    
    public DModSet<T> times(T sq, GradedModule<T> module)
    {
        DModSet<T> ret = new DModSet<T>();
        for(Map.Entry<Dot<T>,Integer> e1 : entrySet()) {
            Dot<T> d = e1.getKey();
            DModSet<T> prod = module.act(d, sq);
            for(Map.Entry<Dot<T>,Integer> e2 : prod.entrySet())
                ret.add(e2.getKey(), e1.getValue() * e2.getValue());
        }
        return ret;
    }
}
