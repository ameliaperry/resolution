package res.algebratypes;

import java.util.*;
import res.utils.*;

public abstract class AbstractGradedVectorSpace<T> extends AbstractMultigradedVectorSpace<T> implements GradedVectorSpace<T>
{
    @Override public abstract Iterable<T> basis(int deg);
    @Override public int num_gradings() { return 1; }
    @Override public Collection<T> basis(int[] i) {
        return basis(i[0]);
    }

    public Iterable<ModSet<T>> basis_wrap(final int deg)
    {
        return new MapIterable(basis(deg), new Func<T,ModSet<T>>() {
            @Override public ModSet<T> run(T t) {
                return new ModSet<T>(t);
            }
        });
    }
}

