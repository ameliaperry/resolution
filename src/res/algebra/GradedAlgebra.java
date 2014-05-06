package res.algebra;

import res.ModSet;

public interface GradedAlgebra<T extends GradedElement<T>>
{
    Iterable<T> basis(int d);
    ModSet<T> times(T a, T b);
    T unit();
}

