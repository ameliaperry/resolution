package res.utils;

public final class MapIterable<T,U> extends Iterable<U>
{
    final Func<T,U> map;
    final Iterable<T> input;
    MapIterable(Iterable<T> input, Func<T,U> map) {
        this.map = map;
        this.input = input;
    }

    @Override public Iterator<U> iterator() {
        return new Iterator<U>() {
            Iterator<T> under = input.iterator();
            @Override boolean hasNext() {
                return under.hasNext();
            }
            @Override Sq next() {
                return map.run(under.hasNext());
            }
            @Override public void remove() {
                under.remove();
            }
        }
    }
}

