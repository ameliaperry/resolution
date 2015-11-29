package res.algebras;

import res.*;
import java.util.*;

public class Sphere<T extends GradedElement<T>> extends GradedModule<T,T>
{
    T unit;

    public Sphere(T unit)
    {
        this.unit = unit;
        /* XXX should follow the number of extra gradings on alg */
    }

    @Override public Iterable<T> basis(int deg) {
        if(deg != 0) return Collections.emptyList();
        else return Collections.singleton(unit);
    }

    @Override public ModSet<T> times(T o, T sq)
    {
        ModSet<T> ret = new DModSet<T>();
        if(sq.equals(unit))
            ret.add(unit,1);
        return ret;
    }
}

