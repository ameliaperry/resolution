package res.algebratypes;

import res.utils.*;

public abstract class AbstractMultigradedVectorSpace<T> implements MultigradedVectorSpace<T>
{
    @Override public abstract int num_gradings();
    @Override public abstract Collection<T> gens(int[] i);

    public Iterable<DModSet<T>> basis_wrap(final int deg)
    {
        return new MapIterable<T,DModSet<T>>(basis(deg), new Func<T,DModSet<T>>() {
            @Override public DModSet<T> run(T t) {
                return new DModSet<T>(t);
            }
        });
    }

}

