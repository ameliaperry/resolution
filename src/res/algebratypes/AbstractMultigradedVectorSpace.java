package res.algebratypes;

import res.utils.*;
import java.util.Collection;

public abstract class AbstractMultigradedVectorSpace<T> implements MultigradedVectorSpace<T>
{
    @Override public abstract int num_gradings();
    @Override public abstract Iterable<T> gens(int[] i);

    public Iterable<ModSet<T>> gens_wrap(final int[] deg)
    {
        return new MapIterable<T,ModSet<T>>(gens(deg), new Func<T,ModSet<T>>() {
            @Override public ModSet<T> run(T t) {
                return new ModSet<T>(t);
            }
        });
    }

}

