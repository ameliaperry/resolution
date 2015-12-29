package res.algebratypes;

import java.util.*;

public abstract class MultigradedAlgebraComputation<T> extends MultigradedComputation<T> implements MultigradedAlgebra<T>
{
    public abstract ModSet<T> times(T a, T b);
    @Override public List<T> distinguished() {
        return new ArrayList<T>();
    }
}

