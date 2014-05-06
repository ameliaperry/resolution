package res;

import res.algebra.*;

import java.util.Collection;

public interface Backend<T extends GradedElement<T>>
{
    boolean isComputed(int s, int t);
    Collection<Generator<T>> gens(int s, int t);

    void register_listener(PingListener p);
    void start();
}
