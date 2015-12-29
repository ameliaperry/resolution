package res.utils;

import java.util.*;

public final class MapIterable<T,U> implements Iterable<U>
{
    final Func<T,U> map;
    final Iterable<T> input;
    public MapIterable(Iterable<T> input, Func<T,U> map) {
        this.map = map;
        this.input = input;
    }

    @Override public Iterator<U> iterator() {
        return new Iterator<U>() {
            Iterator<T> under = input.iterator();
            @Override public boolean hasNext() {
                return under.hasNext();
            }
            @Override public U next() {
                return map.run(under.next());
            }
            @Override public void remove() {
                under.remove();
            }
        };
    }
}

