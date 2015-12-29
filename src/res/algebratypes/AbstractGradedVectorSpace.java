package res.algebratypes;

import java.util.*;
import res.utils.*;

public abstract class AbstractGradedVectorSpace<T> extends AbstractMultigradedVectorSpace<T> implements GradedVectorSpace<T>
{
    @Override public abstract Iterable<T> gens(int deg);
    @Override public int num_gradings() { return 1; }
    @Override public Iterable<T> gens(int[] i) {
        return gens(i[0]);
    }

    public Iterable<ModSet<T>> gens_wrap(final int deg)
    {
        return new MapIterable<T,ModSet<T>>(gens(deg), new Func<T,ModSet<T>>() {
            @Override public ModSet<T> run(T t) {
                return new ModSet<T>(t);
            }
        });
    }
}

