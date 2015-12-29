package res.algebratypes;

import java.util.*;

public abstract class MultigradedComputation<T> implements PingListener
{
    public final static int STATE_NOT_COMPUTED = 0;
    public final static int STATE_FORMALLY_VANISHES = 1;
    public final static int STATE_QUEUED = 2;
    public final static int STATE_STARTED = 3;
    public final static int STATE_OK_TO_QUERY = 4; // not a true state, just a cutoff: >= means ok to query
    public final static int STATE_PARTIAL = 4;
    public final static int STATE_DONE = 5;
    /* backend-specific states start at 10 */

    private Set<PingListener> listeners = new HashSet<PingListener>();
    public Object gens_lock = new Object();

    public abstract int num_gradings();
    public abstract int getState(int[] i); /* what's the state of computation for the given multi-index */
    public abstract Iterable<T> gens(int[] i);
    public void start() { }

    /* the ping mechanism is to receive updates when the state of a certain multi-index changes */
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

