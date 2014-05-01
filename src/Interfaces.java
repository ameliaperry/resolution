
interface ResBackend {
    
    boolean isComputed(int s, int t);
    DModSet[] gimg(int s, int t);
    int[] novikov_grading(int s, int t);

    void register_listener(PingListener p);
    void start();
}

interface PingListener {
    void ping();
}
