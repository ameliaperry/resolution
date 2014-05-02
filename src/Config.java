
class Config
{
    static final int P = 2;
//    static final int P = 3;
   
//    static final int THREADS = 1;
    static final int THREADS = Runtime.getRuntime().availableProcessors() + 1;
    static final boolean DEBUG_THREADS = false;

    static final int T_CAP = 1000; /* no penalties for setting this high */
    static final int MAX_DISPLAY = 200; /* performance penalties in the 2D display for setting this high */

    static final boolean DEBUG = false;
    static final boolean MATRIX_DEBUG = false;
    static final boolean MICHAEL_MODE = true;
    static final boolean STDOUT = false;
    static final boolean TIMING = true;
}
