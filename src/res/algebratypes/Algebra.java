package res.algebratypes;

import java.util.List;

public interface Algebra<T> extends Module<T,T>
{
    T unit();
    List<T> distinguished();
}
