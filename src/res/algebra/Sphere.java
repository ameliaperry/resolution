package res.algebra;

import res.*;
import java.util.*;

public class Sphere<T extends GradedElement<T>> extends GradedModule<T>
{
    private Dot<T> d;
    T unit;

    public Sphere(GradedAlgebra<T> alg)
    {
        Generator<T> g = new Generator<T>(-1,0,0);
        unit = alg.unit();
        d = new Dot<T>(g, alg.unit());
        d.nov = 0;
    }

    @Override public Iterable<Dot<T>> basis(int deg) {
        List<Dot<T>> ret = new ArrayList<Dot<T>>();
        if(deg == 0)
            ret.add(d);
        return ret;
    }

    @Override public DModSet<T> act(Dot<T> o, T sq)
    {
        DModSet<T> ret = new DModSet<T>();
        if(sq.equals(unit))
            ret.add(d,1);
        return ret;
    }
}
