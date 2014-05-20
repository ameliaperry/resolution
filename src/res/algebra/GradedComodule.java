package res.algebra;

import res.*;
import java.util.*;

public abstract class GradedComodule<T extends GradedElement<T>>
{
    abstract public Iterable<Dot<T>> basis(int deg);
    abstract public ModSet<Pair<Dot<T>,T>> act(Dot<T> o);

    public Iterable<DModSet<T>> basis_wrap(final int deg)
    {
        return new Iterable<DModSet<T>>() {
            @Override public Iterator<DModSet<T>> iterator() {
                return new Iterator<DModSet<T>>() {
                    Iterator<Dot<T>> underlying = basis(deg).iterator();
                    @Override public boolean hasNext() { return underlying.hasNext(); }
                    @Override public DModSet<T> next() { return new DModSet<T>(underlying.next()); }
                    @Override public void remove() { underlying.remove(); }
                };
            }
        };
    }
}


