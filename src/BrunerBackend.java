import java.util.*;
import java.util.concurrent.*;


/* Computes Ext_A^{s,t} (M, Z/2) through a minimal resolution of M. */
/* This seems to work effectively through about t=75, and then becomes prohibitively slow. */

class BrunerBackend implements ResBackend
{
    PingListener listener = null;

    HashMap<String,BrunerCellData> output = new HashMap<String,BrunerCellData>();
    static String keystr(int s, int t) {
        return s+","+t;
    }

    /* convenience methods for cell data lookup */
    BrunerCellData dat(int s, int t) {
        return output.get(keystr(s,t));
    }
    int ngens(int s, int t) {
        BrunerCellData dat = output.get(keystr(s,t));
        if(dat == null) return -1;
        return dat.gens.length;
    }
    Collection<DModSet> kbasis(int s, int t) {
        BrunerCellData dat = output.get(keystr(s,t));
        if(Config.DEBUG) Main.die_if(dat == null, "Data null in ("+s+","+t+")");
        return dat.kbasis;
    }
    @Override public Dot[] gens(int s, int t) {
        BrunerCellData dat = output.get(keystr(s,t));
        if(dat == null) return null;
        return dat.gens;
    }
    @Override public boolean isComputed(int s, int t) {
        if(s < 0)
            return true;
        return output.containsKey(keystr(s,t));
    }



    /* task management */
    long start;
    BlockingQueue<ResTask> tasks;

    @Override public void start()
    {
        if(Config.TIMING) start = System.currentTimeMillis();

        tasks = new PriorityBlockingQueue<ResTask>();
        claims = new HashSet<String>();
        putTask(new ResTask(ResTask.COMPUTE, 0, 0));

        for(int i = 0; i < Config.THREADS; i++)
            new BrunerResTaskThread(this).start();
    }

    HashSet<String> claims;
    private boolean atomic_claim_grid(int s, int t)
    {
        String key = keystr(s,t);
        synchronized(claims) {
            if(claims.contains(key))
                return false;
            claims.add(key);
        }
        return true;
    }
    
    void putTask(ResTask t)
    {
        while(true) {
            try {
                tasks.put(t);
                return;
            } catch(InterruptedException e) {
                continue;
            }
        }
    }


    void compute(int s, int t)
    {
        if(Config.DEBUG) System.out.printf("(%d,%d)\n", s,t);
        /* get the old kernel basis */
        BrunerCellData olddat = dat(s-1, t);
        Collection<DModSet> okbasis;
        if(s == 0 && t == 0) {
            okbasis = new ArrayList<DModSet>();
            Dot first = new Dot(0,0,0);
            first.nov = 0;
            okbasis.add(new DModSet(first));
        }
        else if(s == 0)
            okbasis = new ArrayList<DModSet>();
        else
            okbasis = olddat.kbasis;

        Map<Dot,DModSet> list_x = new HashMap<Dot,DModSet>();
        Map<Dot,DModSet> list_dx = new HashMap<Dot,DModSet>();
        ArrayList<DModSet> ker = new ArrayList<DModSet>();
        /* loop over existing dots in this bidegree */
        for(int bt = s; bt < t; bt++) {
            if(Config.DEBUG && gens(s,bt) == null)
                System.out.printf("null gens at (%d,%d)\n", s, bt);
            if(Config.DEBUG) System.out.printf("%d gens at (%d,%d)\n", gens(s,bt).length, s, bt);

            for(Dot b : gens(s,bt)) {
                for(Sq q : Sq.steenrod(t-bt)) {
                    DModSet x = new DModSet(new Dot(b,q));
                    /* compute the image */
                    DModSet dx;
                    if(s > 0) dx = b.img.times(q);
                    else dx = new DModSet();
                    if(Config.DEBUG) System.out.printf("1: %s --> %s", x, dx);

                    /* reduce against the existing image */
                    while(! dx.isEmpty()) {
                        Map.Entry<Dot,Integer> high = dx.lastEntry();
                        Dot d = high.getKey();
                        Integer coeff = high.getValue();

                        DModSet modx = list_x.get(d);
                        if(modx == null)
                            break;

                        x.add(modx, -coeff);
                        dx.add(list_dx.get(d), -coeff);
                        if(Config.DEBUG) System.out.printf(" reduces to %s --> %s", x, dx);
                    }
                    if(Config.DEBUG) System.out.println();

                    if(dx.isEmpty()) { /* dx = 0, add to kernel */
                        if(Config.DEBUG) System.out.printf("Adding %s to kernel\n", x);
                        ker.add(x);
                    } else { /* register this as the entry with highest dot <highest> */
                        Dot highest = dx.lastKey();
                        if(Config.DEBUG) System.out.println("highest term "+highest);
                        if(Config.DEBUG) Main.die_if(list_x.containsKey(highest), "key clash on "+highest);
                        list_x.put(highest, x);
                        list_dx.put(highest, dx);
                    }
                }
            }
        }

        if(Config.DEBUG) {
            System.out.println("Dump of image:");
            for(Dot d : list_x.keySet())
                System.out.printf("%s : %s --> %s\n", d, list_x.get(d), list_dx.get(d));
        }
        
        list_x = null; /* free some memory */

        /* now see how we're doing with respect to the old kernel. modifies okbasis elements */
        ArrayList<Dot> gens = new ArrayList<Dot>();
        for(DModSet k : okbasis) {
            if(Config.DEBUG) System.out.printf("kernel element %s ", k);
            /* reduce against the image */
            while(! k.isEmpty()) {
                Map.Entry<Dot,Integer> ent = k.lastEntry();
                Dot d = ent.getKey();
                Integer coeff = ent.getValue();

                DModSet moddx = list_dx.get(d);
                if(moddx == null)
                    break;
                k.add(moddx, -coeff);
            }
            if(Config.DEBUG) System.out.printf("reduces to %s\n", k);

            if(k.isEmpty()) { /* successfully killed this kernel class */
                if(Config.DEBUG) System.out.println("has been already killed");
                continue;
            }

            if(Config.DEBUG) System.out.printf("adding a generator to kill %s\n", k);
            
            /* haven't yet killed this kernel class -- add a generator */
            Dot gen = new Dot(s, t, gens.size());
            gen.img = k;

            /* add this into the existing image */
            list_dx.put(k.lastKey(), k);

            /* compute the novikov filtration */
            if(Config.MICHAEL_MODE) {
                gen.nov = -1;
                for(Dot o : k.keySet()) 
                    if(gen.nov == -1 || o.nov < gen.nov)
                        gen.nov = o.nov;
                if(Config.STDOUT) System.out.println("generator has extra grading "+gen.nov);
            }
            gens.add(gen);
        }

        /* okbasis is done (and modified), free it */
        if(olddat != null)
            olddat.kbasis = null;

        BrunerCellData dat = new BrunerCellData(gens.toArray(new Dot[] {}), ker);
        output.put(keystr(s,t), dat);

        print_result(t);
        if(Config.STDOUT) System.out.printf("(%2d,%2d): %2d gen, %2d ker\n\n", s, t, dat.gens.length, dat.kbasis.size());
        if(listener != null)
            listener.ping();

        if(Config.TIMING && s == t) {
            long elapsed = System.currentTimeMillis() - start;
            double log = Math.log(elapsed);
            double score = log / t; 
            Runtime run = Runtime.getRuntime();
            System.out.printf("t=%d elapsed=%dms log/t=%f mem=%dM/%dM\n", t, elapsed, score, (run.maxMemory() - run.freeMemory())>>20, run.maxMemory() >> 20);
        }

        if(s < t && (t == s+1 || isComputed(s+1, t-1))) 
            if(atomic_claim_grid(s+1,t))
                putTask(new ResTask(ResTask.COMPUTE, s+1, t)); /* move up-left */
        if(isComputed(s-1, t+1))
            if(atomic_claim_grid(s,t+1))
                putTask(new ResTask(ResTask.COMPUTE, s, t+1)); /* move right */
    }
    


    private void print_result(int s_max)
    {
        if(!Config.STDOUT) return;
        for(int s = s_max; s >= 0; s--) {
            for(int t = s; ; t++) {
                int n = ngens(s,t);
                if(n < 0) break;
                if(n > 0)
                    System.out.printf("%2d ", n);
                else
                    System.out.print("   ");
            }
            System.out.println("###");
        }
    }


    public void register_listener(PingListener p)
    {
        listener = p;
    }
}

class BrunerResTaskThread extends Thread
{
    BrunerBackend back;
    int id;
    volatile static int ids;
    BrunerResTaskThread(BrunerBackend back) {
        setPriority(Thread.MIN_PRIORITY);
        this.back = back;
        id = ids++;
    }

    @Override public void run()
    {
        while(true) {
            if(Config.DEBUG_THREADS) System.out.println(id + ": Waiting for task...");
            ResTask t;
            try {
                t = back.tasks.take();
            } catch(InterruptedException e) {
                continue;
            }
            if(Config.DEBUG_THREADS) System.out.println(id + ": got task ...");

            if(t.t > Config.T_CAP)
                continue;

            switch(t.type) {
                case ResTask.COMPUTE:
                    back.compute(t.s,t.t);
                    break;
                default:
                    Main.die_if(true, "Bad task type.");
            }
        }
    }
}

class BrunerCellData
{
    Dot[] gens;
    Collection<DModSet> kbasis; /* kernel basis dot-sums in bidegree s,t */

    BrunerCellData() { }
    BrunerCellData(Dot[] g, Collection<DModSet> k) {
        gens = g;
        kbasis = k;
    }
}
