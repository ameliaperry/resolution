package res.backend;

import res.*;
import res.algebra.*;
import java.util.*;
import java.util.concurrent.*;


/* Computes Ext_A^{s,t} (M, Z/2) through a minimal resolution of M. */
/* This seems to work effectively through about t=75, and then becomes prohibitively slow. */

public class BrunerBackend<T extends GradedElement<T>> implements Backend<T>
{
    private PingListener listener = null;

    private final GradedAlgebra<T> alg;
    private GradedModule<T> module;


    public BrunerBackend(GradedAlgebra<T> alg) {
        this.alg = alg;
        module = new Sphere<T>(alg);
    }

    private HashMap<Integer,BrunerCellData<T>> output = new HashMap<Integer,BrunerCellData<T>>();
    private static Integer output_key(int s, int t) { return (s<<16) ^ t; }

    /* methods for cell data lookup */
    @Override public Collection<Generator<T>> gens(int s, int t) {
        BrunerCellData<T> dat;
        synchronized(output) {
            dat = output.get(output_key(s,t));
        }
        if(dat == null) return null;
        return dat.gens;
    }
    @Override public boolean isComputed(int s, int t) {
        if(s < 0)
            return true;
        return gens(s,t) != null;
    }
    private BrunerCellData<T> dat(int s, int t) {
        synchronized(output) {
            return output.get(output_key(s,t));
        }
    }
    private void putOutput(int s, int t, BrunerCellData<T> dat) {
        synchronized(output) {
            output.put(output_key(s,t), dat);
        }
    }



    /* task management */
    long start;
    BlockingQueue<BrunerResTask> tasks;

    @Override public void start()
    {
        if(Config.TIMING) start = System.currentTimeMillis();

        tasks = new PriorityBlockingQueue<BrunerResTask>();
        claims = new HashSet<Integer>();
        putTask(new BrunerResTask(BrunerResTask.COMPUTE, 0, 0));

        for(int i = 0; i < Config.THREADS; i++)
            new BrunerResTaskThread(this).start();
    }

    HashSet<Integer> claims;
    private boolean atomic_claim_grid(int s, int t)
    {
        Integer key = output_key(s,t);
        synchronized(claims) {
            if(claims.contains(key))
                return false;
            claims.add(key);
        }
        return true;
    }
    
    private void putTask(BrunerResTask t)
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

    
    /* math */

    void compute(int s, int t)
    {
        if(Config.DEBUG) System.out.printf("(%d,%d)\n", s,t);
        /* get the old kernel basis */
        BrunerCellData<T> olddat = dat(s-1, t);
        Iterable<DModSet<T>> okbasis;
        
        if(s == 0)
            okbasis = module.basis_wrap(t);
        else
            okbasis = olddat.kbasis;

        Map<Dot<T>,DModSet<T>> list_x = new HashMap<Dot<T>,DModSet<T>>();
        Map<Dot<T>,DModSet<T>> list_dx = new HashMap<Dot<T>,DModSet<T>>();
        ArrayList<DModSet<T>> ker = new ArrayList<DModSet<T>>();
        /* loop over existing dots in this bidegree */
        for(int gt = s; gt < t; gt++) {
            if(Config.DEBUG && gens(s,gt) == null)
                System.out.printf("null gens at (%d,%d)\n", s, gt);
            if(Config.DEBUG) System.out.printf("%d gens at (%d,%d)\n", gens(s,gt).size(), s, gt);

            for(Generator<T> g : gens(s,gt)) {
                for(T q : alg.basis(t-gt)) {
                    DModSet<T> x = new DModSet<T>(new Dot<T>(g,q));
                    /* compute the image */
                    DModSet<T> dx;
                    if(s > 0) dx = g.img.times(q, alg);
                    else dx = g.img.times(q, module);
                    if(Config.DEBUG) System.out.printf("1: %s --> %s", x, dx);

                    /* reduce against the existing image */
                    while(! dx.isEmpty()) {
                        Map.Entry<Dot<T>,Integer> high = dx.lastEntry();
                        Dot<T> d = high.getKey();
                        Integer coeff = high.getValue();

                        DModSet<T> modx = list_x.get(d);
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
                        Dot<T> highest = dx.lastKey();
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
            for(Dot<T> d : list_x.keySet())
                System.out.printf("%s : %s --> %s\n", d, list_x.get(d), list_dx.get(d));
        }
        
        /* free some memory */
        list_x = null;

        /* save the kernel data */
        BrunerCellData<T> dat = new BrunerCellData<T>(null, ker);
        putOutput(s, t, dat);
        
        /* kick off the first child task -- only depends ker, not gens */
        if(s < t && (t == s+1 || isComputed(s+1, t-1))) 
            if(atomic_claim_grid(s+1,t))
                putTask(new BrunerResTask(BrunerResTask.COMPUTE, s+1, t)); /* move up-left */
        

        /* now see how we're doing with respect to the old kernel. modifies okbasis elements */
        ArrayList<Generator<T>> gens = new ArrayList<Generator<T>>();
        for(DModSet<T> k : okbasis) {
            if(Config.DEBUG) System.out.printf("kernel element %s ", k);
            /* reduce against the image */
            while(! k.isEmpty()) {
                Map.Entry<Dot<T>,Integer> ent = k.lastEntry();
                Dot<T> d = ent.getKey();
                Integer coeff = ent.getValue();

                DModSet<T> moddx = list_dx.get(d);
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
            Generator<T> gen = new Generator<T>(s, t, gens.size());
            gen.img = k;

            /* add this into the existing image */
            list_dx.put(k.lastKey(), k);

            /* compute the novikov filtration */
            gen.nov = -1;
            for(Dot<T> o : k.keySet()) 
                if(gen.nov == -1 || o.nov < gen.nov)
                    gen.nov = o.nov;
            if(Config.STDOUT) System.out.println("generator has extra grading "+gen.nov);

            gens.add(gen);
        }

        /* okbasis is done (and modified), free it */
        if(olddat != null)
            olddat.kbasis = null;

        /* save the result -- at this point the computation is considered finished */
        dat.gens = gens;


        if(Config.STDOUT) System.out.printf("(%2d,%2d): %2d gen, %2d ker\n\n", s, t, dat.gens.size(), dat.kbasis.size());
        if(listener != null)
            listener.ping(s,t);

        if(Config.TIMING && s == t) {
            long elapsed = System.currentTimeMillis() - start;
            double log = Math.log(elapsed);
            double score = log / t; 
            Runtime run = Runtime.getRuntime();
            System.out.printf("t=%d elapsed=%dms log/t=%f mem=%dM/%dM\n",
                t, elapsed, score, (run.maxMemory() - run.freeMemory())>>20, run.maxMemory()>>20);
        }

        /* kick off the second task */
        if(isComputed(s-1, t+1))
            if(atomic_claim_grid(s,t+1))
                putTask(new BrunerResTask(BrunerResTask.COMPUTE, s, t+1)); /* move right */
    }
    

    /* admin */

    public void setModule(GradedModule<T> m)
    {
        Main.die_if(isComputed(0,0), "Attempted to change resolving module after computation began.");
        module = m;
    }

    @Override public void register_listener(PingListener p)
    {
        listener = p;
    }
}

class BrunerResTaskThread extends Thread
{
    BrunerBackend<?> back;
    int id;
    volatile static int ids;
    BrunerResTaskThread(BrunerBackend<?> back) {
        setPriority(Thread.MIN_PRIORITY);
        this.back = back;
        id = ids++;
    }

    @Override public void run()
    {
        while(true) {
            if(Config.DEBUG_THREADS) System.out.println(id + ": Waiting for task...");
            BrunerResTask t;
            try {
                t = back.tasks.take();
            } catch(InterruptedException e) {
                continue;
            }
            if(Config.DEBUG_THREADS) System.out.println(id + ": got task ...");

            if(t.t > Config.T_CAP)
                continue;

            switch(t.type) {
                case BrunerResTask.COMPUTE:
                    back.compute(t.s,t.t);
                    break;
                default:
                    Main.die_if(true, "Bad task type.");
            }
        }
    }
}

class BrunerResTask implements Comparable<BrunerResTask>
{
    /* task types */
    final static int COMPUTE = 0;

    int type;
    int s;
    int t;

    BrunerResTask(int type, int s, int t)
    {
        this.type = type;
        this.s = s;
        this.t = t;
    }

    @Override public int compareTo(BrunerResTask o)
    {
        return t - o.t;
    }
}

class BrunerCellData<T extends GradedElement<T>>
{
    Collection<Generator<T>> gens;
    Collection<DModSet<T>> kbasis; /* kernel basis dot-sums in bidegree s,t */

    BrunerCellData() { }
    BrunerCellData(Collection<Generator<T>> g, Collection<DModSet<T>> k) {
        gens = g;
        kbasis = k;
    }
}
