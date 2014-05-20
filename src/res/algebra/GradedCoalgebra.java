package res.algebra;

public interface GradedCoalgebra<T extends GradedElement<T>>
{
    Iterable<T> basis(int d);
    ModSet<Pair<T,T>> diagonal(T a);
    T unit();
    int extraDegrees();
}

