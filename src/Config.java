
class Config
{
    /* configurable from settings dialog; these set defaults */
    static int P;
    static int THREADS = 1;
    static int T_CAP = 100;
    static boolean MICHAEL_MODE = true;

    /* not configurable from settings dialog */
    static final boolean DEBUG = false;
    static final boolean DEBUG_THREADS = false;
    static final boolean MATRIX_DEBUG = false;
    static final boolean STDOUT = false;
    static final boolean TIMING = true;

    /* the following aren't actually config */
    static int Q;
}
