package res.algebras;

import res.algebratypes.*;
import java.util.Collections;

public class Sphere<T extends GradedElement<T>> extends AbstractGradedModule<T,T>
{
    T unit;

    public Sphere(T unit)
    {
        this.unit = unit;
        /* XXX should follow the number of extra gradings on alg */
    }

    @Override public Iterable<T> gens(int deg) {
        if(deg != 0) return Collections.emptyList();
        else return Collections.singleton(unit);
    }

    @Override public ModSet<T> times(T o, T sq)
    {
        ModSet<T> ret = new ModSet<T>();
        if(sq.equals(unit))
            ret.add(unit,1);
        return ret;
    }
}

