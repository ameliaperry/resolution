package res.algebra;

import java.util.*;

public abstract class MultigradedVectorSpace<T extends MultigradedElement<T>> implements PingListener
{
    private Set<PingListener> listeners = new HashSet<PingListener>();

    public abstract boolean isComputed(int[] i);
    public abstract boolean isVanishing(int[] i); /* is this in a vanishing area? */
    public abstract Collection<T> gens(int[] i);
    public abstract int num_gradings();
    public void start() { }

    public void addListener(PingListener l) {
        listeners.add(l);
    }
    public void removeListener(PingListener l) {
        listeners.remove(l);
    }
    @Override public void ping(int[] i) {
        for(PingListener l : listeners)
            l.ping(i);
    }
}

