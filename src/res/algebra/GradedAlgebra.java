package res.algebra;

public interface GradedAlgebra<T extends GradedElement<T>>
{
    Iterable<T> basis(int d);
    ModSet<T> times(T a, T b);
    T unit();
    int extraDegrees();
}

