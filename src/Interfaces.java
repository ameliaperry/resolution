
interface ResBackend {
    
    boolean isComputed(int s, int t);
    Dot[] gens(int s, int t);

    void register_listener(PingListener p);
    void start();
}

interface PingListener {
    void ping(int s, int t);
}
