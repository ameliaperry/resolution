package res.algebra;

public abstract class MultigradedAlgebra<T extends MultigradedElement<T>> extends MultigradedVectorSpace<T>
{
    public abstract ModSet<T> times(T a, T b);
}

