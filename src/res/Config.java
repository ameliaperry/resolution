package res;

public class Config
{
    /* configurable from settings dialog; these set defaults */
    public static int P = 2;
    public static int THREADS = 1;
    public static int T_CAP = 100;
    public static boolean MICHAEL_MODE = false;
    public static boolean MOTIVIC_GRADING = false;

    /* not configurable from settings dialog */
    public static final boolean DEBUG = false;
    public static final boolean DEBUG_THREADS = false;
    public static final boolean MATRIX_DEBUG = false;
    public static final boolean STDOUT = false;
    public static final boolean TIMING = true;

    /* the following aren't actually config */
    public static int Q;
}
