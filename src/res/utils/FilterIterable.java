package res.utils;

import java.util.*;

public final class FilterIterable<T> implements Iterable<T>
{
    final Func<T,Boolean> filter;
    final Iterable<T> input;
    public FilterIterable(Iterable<T> input, Func<T,Boolean> filter) {
        this.filter = filter;
        this.input = input;
    }

    @Override public Iterator<T> iterator() {
        return new Iterator<T>() {
            Iterator<T> under = input.iterator();
            T nxt = null;
            @Override public boolean hasNext() {
                while(under.hasNext()) {
                    nxt = under.next();
                    if(filter.run(nxt)) return true;
                }
                nxt = null;
                return false;
            }
            @Override public T next() {
                if(nxt == null && hasNext() == false) throw new NoSuchElementException();
                return nxt;
            }
            @Override public void remove() {
                under.remove();
            }
        };
    }
}

