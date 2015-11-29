package res.algebratypes;

import java.util.*;

public abstract class AbstractGradedAlgebra<T> extends AbstractGradedModule<T> implements GradedAlgebra<T>
{
    @Override public abstract ModSet<T> times(T a, T b);
    @Override public abstract T unit();
    @Override public abstract List<T> distinguished();
    @Override public abstract DModSet<T> act(Dot<T> a, T sq) {
        DModSet<T> ret = new DModSet<T>();
        for(Map.Entry<T,Integer> e : times(a.sq, sq))
            ret.add(new Dot(a.base, e.key), e.value);
        return ret;
    }
}

