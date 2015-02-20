package res.algebra;

import res.*;
import java.util.*;

public class Sphere<T extends GradedElement<T>> extends GradedModule<T>
{
    private Dot<T> d;
    T unit;

    public Sphere(T unit)
    {
        this.unit = unit;
        /* XXX should follow the number of extra gradings on alg */
        Generator<T> g = new Generator<T>(new int[] {-1,0,0}, 0);
        d = new Dot<T>(g, unit);
    }

    @Override public Iterable<Dot<T>> basis(int deg) {
        if(deg != 0) return Collections.emptyList();
        else return Collections.singleton(d);
    }

    @Override public DModSet<T> act(Dot<T> o, T sq)
    {
        DModSet<T> ret = new DModSet<T>();
        if(sq.equals(unit))
            ret.add(d,1);
        return ret;
    }
}
