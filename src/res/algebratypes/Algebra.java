package res.algebratypes;

public interface Algebra<T> extends Module<T,T>
{
    T unit();
    List<T> distinguished();
}
