package res.algebra;

import java.util.*;

public interface GradedAlgebra<T extends GradedElement<T>>
{
    Iterable<T> basis(int d);
    ModSet<T> times(T a, T b);
    T unit();
    List<T> distinguished();
    int extraDegrees();
}

