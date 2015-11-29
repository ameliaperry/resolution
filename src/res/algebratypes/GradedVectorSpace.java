package res.algebratypes;

public interface GradedVectorSpace<T>
{
    public Iterable<Dot<T>> basis(int deg);
}

