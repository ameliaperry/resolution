package res.algebratypes;

public abstract class AbstractGradedModule<T,U> extends AbstractGradedVectorSpace<T> implements GradedModule<T,U>
{
    @Override public abstract ModSet<T> times(T o, U sq);
}

