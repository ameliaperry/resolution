package res.algebratypes;

public abstract class AbstractMultigradedModule<T,U> extends AbstractMultigradedVectorSpace<T> implements MultigradedModule<T,U>
{
    @Override public abstract ModSet<T> times(T o, U sq);
}

