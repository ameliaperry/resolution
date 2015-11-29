package res.utils;

public final class FilterIterable<T> extends Iterable<T>
{
    final Func<T,Boolean> filter;
    final Iterable<T> input;
    FilterIterable(Iterable<T> input, Func<T,Boolean> filter) {
        this.filter = filter;
        this.input = input;
    }

    @Override public Iterator<Sq> iterator() {
        return new Iterator<T>() {
            Iterator<T> under = input.iterator();
            T next = null;
            @Override boolean hasNext() {
                while(under.hasNext()) {
                    next = under.next();
                    if(filter.run(next)) return true;
                }
                next = null;
                return false;
            }
            @Override Sq next() {
                if(next == null && hasNext() == false) throw new NoSuchElementException();
                return next;
            }
            @Override public void remove() {
                under.remove();
            }
        };
    }
}

